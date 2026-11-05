/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.subscription

import com.kelavs.vpn.network.SafeHttpClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonPrimitive as jsonPrim
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.Buffer
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.Base64

/**
 * Parses WireGuard subscriptions in every format that real providers (Hiddify, Marzban,
 * WG-portal, plain .conf hosting, ...) actually emit:
 *
 * 1. plain `[Interface] … [Peer]` text, one or many blocks
 * 2. Base64 of that text — standard or URL-safe alphabet, padded or not
 * 3. `wg://<base64(config)>#name` links
 * 4. `wg://host:port/?pk=…&peer_pk=…&local_address=…` links (Hiddify, WARP)
 * 5. `wireguard://<privateKey>@host:port?address=…&publickey=…` links (Hiddify)
 * 6. a list of URLs, each of which may hold any of the above
 * 7. sing-box JSON with a `wireguard` outbound
 *
 * Anything else (Xray/V2Ray/Clash/HTML) is reported with a precise, actionable message
 * instead of a generic "nothing found", because a WireGuard engine cannot consume those
 * protocols and the user needs to know that.
 */
class SubscriptionParser(
    private val httpClient: OkHttpClient = SafeHttpClient.create(),
    private val maxResponseSize: Long = 10 * 1024 * 1024, // 10 MB
    private val timeoutSeconds: Int = 30,
    /**
     * Allows loopback/private hosts. Kept false in production so that a subscription URL can
     * never be used for SSRF against the user's local network; enabled only by unit tests that
     * run against a local MockWebServer.
     */
    internal val allowPrivateHosts: Boolean = false,
    /** Hard limit on how many nested URL-list entries are downloaded. */
    private val maxNestedFetches: Int = 15
) {

    private companion object {
        const val USER_AGENT = "KelavsVPN/1.1 (Android; WireGuard subscription parser)"
        const val BROWSER_USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 (KHTML, like Gecko) " +
                "Chrome/124.0 Mobile Safari/537.36"
        const val MAX_DEPTH = 3

        val WIREGUARD_SCHEMES = listOf("wg://", "wireguard://")
        val OTHER_VPN_SCHEMES = listOf(
            "vless://", "vmess://", "trojan://", "ss://", "ssr://",
            "hysteria://", "hysteria2://", "hy2://", "tuic://",
            "juicity://", "naive+https://", "socks://", "http3://", "wireguard-v2://"
        )
    }

    /**
     * Downloads and parses a subscription URL.
     */
    suspend fun parseSubscription(url: String): Result<SubscriptionParseResult> {
        return try {
            val validatedUrl = validateAndNormalizeUrl(url)
            var report: ParseReport
            try {
                report = parsePayload(fetch(validatedUrl), validatedUrl, depth = 0)
            } catch (e: SubscriptionParseException) {
                // Some panels reject unknown User-Agents with 403 instead of serving the sub.
                if (e.message?.contains("403") != true) throw e
                report = parsePayload(fetch(validatedUrl, BROWSER_USER_AGENT), validatedUrl, 0)
            }

            // An HTML answer is usually a login/expired page, but a few panels only serve the
            // subscription to a browser-like client, so retry once before giving up.
            if (report.configs.isEmpty() && report.htmlLike) {
                val retry = runCatching {
                    parsePayload(fetch(validatedUrl, BROWSER_USER_AGENT), validatedUrl, 0)
                }.getOrNull()
                if (retry != null && retry.configs.isNotEmpty()) report = retry
            }
            finish(report, validatedUrl)
        } catch (e: SubscriptionParseException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(SubscriptionParseException("خطای غیرمنتظره: ${e.message}", e))
        }
    }

    /**
     * Parses text the user pasted directly (a .conf file, a base64 blob or a wg:// link).
     * No network access except for URL lists, which are resolved normally.
     */
    suspend fun parseText(text: String, sourceLabel: String = "پیکربندی دستی"): Result<SubscriptionParseResult> {
        return try {
            if (text.isBlank()) {
                return Result.failure(SubscriptionParseException("متن پیکربندی خالی است"))
            }
            val report = parsePayload(text, sourceLabel, depth = 0)
            finish(report, sourceLabel)
        } catch (e: SubscriptionParseException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(SubscriptionParseException("خطای غیرمنتظره: ${e.message}", e))
        }
    }

    // ------------------------------------------------------------------ result assembly

    private fun finish(report: ParseReport, sourceUrl: String): Result<SubscriptionParseResult> {
        if (report.configs.isEmpty()) {
            return Result.failure(SubscriptionParseException(report.explain()))
        }
        val unique = removeDuplicates(report.configs)
        val valid = unique.filter { it.isValid() }
        val invalidCount = unique.size - valid.size

        if (valid.isEmpty()) {
            val fields = unique.firstOrNull()?.let { describeInvalid(it) } ?: "نامشخص"
            return Result.failure(
                SubscriptionParseException(
                    "کانفیگ‌ها با موتور WireGuard ناسازگارند (فیلد نامعتبر: $fields)"
                )
            )
        }
        return Result.success(
            SubscriptionParseResult(
                configs = valid,
                sourceUrl = sourceUrl,
                invalidConfigsCount = invalidCount,
                duplicateConfigsCount = report.configs.size - unique.size
            )
        )
    }

    private fun describeInvalid(config: SubscriptionConfig): String = when {
        !config.isValidKey(config.privateKey) -> "PrivateKey"
        !config.isValidKey(config.peerPublicKey) -> "PublicKey"
        !config.isValidEndpoint(config.peerEndpoint) -> "Endpoint=${config.peerEndpoint}"
        config.address.isBlank() -> "Address"
        else -> "AllowedIPs/DNS"
    }

    // ------------------------------------------------------------------ payload parsing

    private fun parsePayload(raw: String, source: String, depth: Int): ParseReport {
        val report = ParseReport()
        if (depth > MAX_DEPTH) {
            report.htmlLike = false
            return report
        }

        val text = stripBom(raw).replace("\r\n", "\n").replace('\r', '\n').trim()
        if (text.isEmpty()) return report
        if (report.preview == null) {
            report.preview = text.lineSequence().take(4).joinToString(" ")
                .replace(Regex("\\s+"), " ").trim().take(160)
        }

        // 1. HTML / JSON error pages
        if (looksLikeHtml(text)) {
            report.htmlLike = true
            report.detectedTitle = regexTitle(text)
            return report
        }

        // 2. WireGuard links inside the payload (before base64: links may look like base64)
        parseWireGuardLinks(text, source, report)

        // 3. sing-box / JSON — a .conf payload also starts with '[', so the shape must be
        //    checked properly or every plain config would be swallowed here.
        if (looksLikeJson(text)) {
            parseSingBoxJson(text, source, report)
        }

        // 4. plain WireGuard blocks
        parseWireGuardBlocks(text, source, report)
        if (report.configs.isNotEmpty()) return report

        // 5. base64 blob (standard or url-safe, padded or not)
        val decoded = decodeBase64Lenient(text)
        if (decoded != null && looksLikeText(decoded)) {
            val nested = parsePayload(decoded, source, depth + 1)
            report.merge(nested)
            if (report.configs.isNotEmpty()) return report
        }

        // 6. list of http(s) URLs
        val urls = extractHttpUrls(text)
        if (urls.isNotEmpty()) {
            report.urlListSize = urls.size
            for (u in urls.take(maxNestedFetches)) {
                val body = runCatching { fetchBlocking(u) }.getOrNull() ?: continue
                report.merge(parsePayload(body, u, depth + 1))
            }
            if (report.configs.isNotEmpty()) return report
        }

        report.rememberSchemes(text)
        return report
    }

    /** `wg://…` and `wireguard://…` links, one per line, optionally with `#Name`. */
    private fun parseWireGuardLinks(text: String, source: String, report: ParseReport) {
        text.lineSequence().forEach { rawLine ->
            val line = rawLine.trim()
            val lower = line.lowercase()
            val at = WIREGUARD_SCHEMES.indexOfFirst { lower.startsWith(it) }
            if (at < 0) return@forEach
            val scheme = WIREGUARD_SCHEMES[at]
            val fragment = line.substringAfter('#', "").trim()
            val payload = line.removePrefix(line.substring(0, scheme.length))
            // cut fragment from payload if present
            val cleanPayload = if (payload.contains('#')) payload.substringBefore('#') else payload
            val config = runCatching { parseWireGuardLink(cleanPayload, fragment, source) }.getOrNull()
            if (config != null) report.configs.add(config)
            else report.linksSeen++
        }
    }

    private fun parseWireGuardLink(payload: String, name: String, source: String): SubscriptionConfig? {
        val trimmed = payload.trim()
        if (trimmed.isEmpty()) return null

        // (a) wg://<base64 of .conf>
        if (!trimmed.contains('@') && !trimmed.contains('?')) {
            val decoded = decodeBase64Lenient(trimmed) ?: return null
            if (!looksLikeText(decoded)) return null
            val temp = ParseReport()
            parseWireGuardBlocks(decoded, source, temp)
            val first = temp.configs.firstOrNull() ?: return null
            return if (name.isBlank()) first else first.copy(name = name)
        }

        // (b) wg://host:port/?pk=…&peer_pk=…  or  wireguard://<privateKey>@host:port?address=…
        val query = trimmed.substringAfter('?', "")
        val params = parseQuery(query)

        val beforeQuery = trimmed.substringBefore('?')
        val authority = beforeQuery.substringBefore('/')

        val privateKeyFromUserInfo = if (authority.contains('@')) {
            authority.substringBeforeLast('@')
        } else {
            null
        }
        val hostPort = authority.substringAfterLast('@')

        val privateKey = listOfNotNull(
            privateKeyFromUserInfo,
            params["pk"], params["privatekey"], params["private_key"], params["secretkey"],
            params["privateKey"]
        ).firstOrNull { it.isNotBlank() }?.let(::percentDecode) ?: return null

        val peerPublicKey = listOfNotNull(
            params["peer_pk"], params["peerpk"], params["peer_public_key"], params["publickey"],
            params["public_key"], params["pubkey"], params["peerPublicKey"]
        ).firstOrNull { it.isNotBlank() }?.let(::percentDecode) ?: return null

        val endpoint = normalizeEndpoint(hostPort) ?: return null

        val address = listOfNotNull(
            params["address"], params["local_address"], params["addrs"], params["ip"],
            params["localaddress"]
        ).firstOrNull { it.isNotBlank() }?.let(::urlDecode)
            ?: params["address"] ?: "10.0.0.2/32"

        val dns = params["dns"]?.let(::urlDecode)
        val mtu = params["mtu"]?.toIntOrNull()
        val keepalive = (params["keepalive"] ?: params["persistentkeepalive"])?.toIntOrNull()
        val psk = (params["pre_shared_key"] ?: params["presharedkey"] ?: params["psk"])
            ?.let(::percentDecode)?.takeIf { it.isNotBlank() }
        val allowedIps = params["allowedips"]?.let(::urlDecode) ?: "0.0.0.0/0, ::/0"
        val reserved = params["reserved"]

        val conf = buildString {
            appendLine("# Kelavs: imported from link")
            appendLine("[Interface]")
            appendLine("PrivateKey = $privateKey")
            appendLine("Address = $address")
            if (!dns.isNullOrBlank()) appendLine("DNS = $dns")
            if (mtu != null) appendLine("MTU = $mtu")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = $peerPublicKey")
            if (!psk.isNullOrBlank()) appendLine("PresharedKey = $psk")
            appendLine("AllowedIPs = $allowedIps")
            appendLine("Endpoint = $endpoint")
            if (keepalive != null) appendLine("PersistentKeepalive = $keepalive")
            if (!reserved.isNullOrBlank()) appendLine("# reserved = $reserved (ignored by WireGuard)")
        }

        return SubscriptionConfig(
            name = name.ifBlank { "Server ${endpoint.substringBeforeLast(':')}" },
            privateKey = privateKey,
            address = address,
            dns = dns,
            mtu = mtu,
            persistentKeepalive = keepalive,
            peerPublicKey = peerPublicKey,
            peerAllowedIPs = allowedIps,
            peerEndpoint = endpoint,
            peerPresharedKey = psk,
            originalConfig = conf.trim()
        )
    }

    /** Plain `[Interface] … [Peer]` blocks; supports several peers per interface. */
    private fun parseWireGuardBlocks(text: String, source: String, report: ParseReport) {
        val normalized = text.replace("\r\n", "\n").replace('\r', '\n')

        val starts = Regex("(?im)^\\s*\\[interface]").findAll(normalized).map { it.range.first }.toList()
        if (starts.isNotEmpty()) {
            starts.forEachIndexed { index, start ->
                val end = starts.getOrNull(index + 1) ?: normalized.length
                val block = normalized.substring(start, end)
                report.configs.addAll(parseSingleBlock(block, source, report.configs.size))
            }
            return
        }

        // No [Interface] header: accept the payload only if it really carries key material.
        val hasKeys = normalized.contains("PrivateKey", true) &&
            normalized.contains("PublicKey", true) &&
            normalized.contains("Endpoint", true)
        if (hasKeys) {
            report.configs.addAll(parseSingleBlock(normalized, source, 0))
        }
    }

    private fun parseSingleBlock(block: String, source: String, index: Int): List<SubscriptionConfig> {
        val interfaceValues = LinkedHashMap<String, String>()
        val peers = mutableListOf<MutableMap<String, String>>()
        var current: MutableMap<String, String>? = null
        var commentName: String? = null

        block.lines().forEach { rawLine ->
            val line = stripComment(rawLine)
            if (line.isBlank()) return@forEach

            when {
                line.trim().startsWith("[", ignoreCase = false) && line.trim().endsWith("]") -> {
                    val section = line.trim().lowercase()
                    current = when (section) {
                        "[peer]" -> mutableMapOf<String, String>().also { peers.add(it) }
                        else -> null
                    }
                }

                rawLine.trim().startsWith("#") || rawLine.trim().startsWith(";") -> {
                    val comment = rawLine.trim().removePrefix("#").removePrefix(";").trim()
                    if (commentName == null && comment.isNotBlank() &&
                        (comment.startsWith("name", true) || comment.contains(" = "))
                    ) {
                        commentName = comment.substringAfter('=').trim().takeIf { it.isNotBlank() && it != comment }
                    }
                }

                else -> {
                    val eq = line.indexOf('=')
                    if (eq <= 0) return@forEach
                    val key = line.substring(0, eq).trim()
                    val value = line.substring(eq + 1).trim()
                    if (key.isEmpty() || value.isEmpty()) return@forEach
                    when {
                        current != null -> current?.put(key, value)
                        else -> interfaceValues[key] = value
                    }
                }
            }
        }

        val privateKey = interfaceValues["PrivateKey"] ?: "PrivateKey".let { interfaceValues[it] }
        val address = interfaceValues["Address"]
        val dns = interfaceValues["DNS"]
        val mtu = interfaceValues["MTU"]?.toIntOrNull()
        val keepaliveIf = interfaceValues["PersistentKeepalive"]?.toIntOrNull()

        if (privateKey.isNullOrBlank() || address.isNullOrBlank()) return emptyList()

        val peerMaps: List<Map<String, String>> = if (peers.isEmpty()) listOf(emptyMap<String, String>()) else peers
        val result = mutableListOf<SubscriptionConfig>()

        peerMaps.forEachIndexed { peerIndex, peer ->
            val publicKey = peer["PublicKey"]?.takeIf { it.isNotBlank() }
                ?: interfaceValues["PublicKey"]?.takeIf { it.isNotBlank() }
                ?: return@forEachIndexed
            val endpoint = (peer["Endpoint"] ?: interfaceValues["Endpoint"])?.let(::normalizeEndpoint)
                ?: return@forEachIndexed
            val allowed = peer["AllowedIPs"] ?: interfaceValues["AllowedIPs"] ?: "0.0.0.0/0, ::/0"

            val name = commentName
                ?: interfaceValues["Name"]
                ?: peer["Name"]
                ?: "Server ${index + peerIndex + 1} - ${endpoint.substringBeforeLast(':').trim('[', ']')}"

            val conf = buildString {
                appendLine("[Interface]")
                appendLine("PrivateKey = $privateKey")
                appendLine("Address = $address")
                if (!dns.isNullOrBlank()) appendLine("DNS = $dns")
                if (mtu != null) appendLine("MTU = $mtu")
                appendLine()
                appendLine("[Peer]")
                appendLine("PublicKey = $publicKey")
                peer["PresharedKey"]?.takeIf { it.isNotBlank() }?.let { appendLine("PresharedKey = $it") }
                appendLine("AllowedIPs = $allowed")
                appendLine("Endpoint = $endpoint")
                val ka = peer["PersistentKeepalive"]?.toIntOrNull() ?: keepaliveIf
                if (ka != null) appendLine("PersistentKeepalive = $ka")
            }

            result.add(
                SubscriptionConfig(
                    name = name,
                    privateKey = privateKey,
                    address = address,
                    dns = dns,
                    mtu = mtu,
                    persistentKeepalive = peer["PersistentKeepalive"]?.toIntOrNull() ?: keepaliveIf,
                    peerPublicKey = publicKey,
                    peerAllowedIPs = allowed,
                    peerEndpoint = endpoint,
                    peerPresharedKey = peer["PresharedKey"]?.takeIf { it.isNotBlank() },
                    originalConfig = conf.trim()
                )
            )
        }
        return result
    }

    /** sing-box style JSON that carries a `wireguard` outbound. */
    private fun parseSingBoxJson(text: String, source: String, report: ParseReport) {
        val root = runCatching { Json.parseToJsonElement(text) }.getOrNull() ?: return
        val candidates = mutableListOf<JsonObject>()
        when (root) {
            is JsonObject -> {
                root["outbounds"]?.let { arrayOfObjects(it, candidates) }
                if (root["type"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() } == "wireguard") candidates.add(root)
            }
            is JsonArray -> arrayOfObjects(root, candidates)
            else -> Unit
        }
        candidates.filter { obj -> obj["type"]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() } == "wireguard" }.forEach { obj ->
            val conf = runCatching { singBoxToConfig(obj, report.configs.size) }.getOrNull() ?: return@forEach
            report.configs.add(conf)
        }
    }

    private fun arrayOfObjects(element: kotlinx.serialization.json.JsonElement, into: MutableList<JsonObject>) {
        runCatching { element.jsonArray.forEach { if (it is JsonObject) into.add(it) } }
    }

    private fun singBoxToConfig(obj: JsonObject, index: Int): SubscriptionConfig? {
        fun str(key: String): String? = obj[key]?.let { runCatching { it.jsonPrimitive.content }.getOrNull() }?.takeIf { it.isNotBlank() }
        val server = str("server") ?: return null
        val port = str("server_port") ?: str("port") ?: return null
        val privateKey = str("private_key") ?: return null
        val peerPublicKey = str("peer_public_key") ?: str("public_key") ?: return null
        val address = obj["local_address"]?.let { element ->
            runCatching { element.jsonArray.joinToString(", ") { e -> e.jsonPrimitive.content } }.getOrNull()
        } ?: str("address") ?: "10.0.0.2/32"
        val mtu = str("mtu")?.toIntOrNull()
        val endpoint = "$server:$port"
        val conf = buildString {
            appendLine("[Interface]")
            appendLine("PrivateKey = $privateKey")
            appendLine("Address = $address")
            if (mtu != null) appendLine("MTU = $mtu")
            appendLine()
            appendLine("[Peer]")
            appendLine("PublicKey = $peerPublicKey")
            str("pre_shared_key")?.let { appendLine("PresharedKey = $it") }
            appendLine("AllowedIPs = 0.0.0.0/0, ::/0")
            appendLine("Endpoint = $endpoint")
        }
        return SubscriptionConfig(
            name = str("tag") ?: "Server ${index + 1} - $server",
            privateKey = privateKey,
            address = address,
            dns = null,
            mtu = mtu,
            persistentKeepalive = null,
            peerPublicKey = peerPublicKey,
            peerAllowedIPs = "0.0.0.0/0, ::/0",
            peerEndpoint = endpoint,
            peerPresharedKey = str("pre_shared_key"),
            originalConfig = conf.trim()
        )
    }

    // ------------------------------------------------------------------ helpers

    private fun decodeBase64Lenient(input: String): String? {
        val compact = input.filterNot { it.isWhitespace() }
        if (compact.length < 40) return null
        if (compact.any { it !in BASE64_CHARS }) return null

        val variants = listOf(
            compact,
            compact.replace('-', '+').replace('_', '/')
        ).distinct()

        for (variant in variants) {
            val padded = when (variant.length % 4) {
                0 -> variant
                2 -> "$variant=="
                3 -> "$variant="
                else -> continue
            }
            val bytes = runCatching { Base64.getDecoder().decode(padded) }.getOrNull() ?: continue
            val text = String(bytes, StandardCharsets.UTF_8)
            val printable = text.count { it == '\n' || it == '\r' || it == '\t' || it >= ' ' }
            if (printable.toDouble() / text.length.coerceAtLeast(1) > 0.95) return text
        }
        return null
    }

    private fun looksLikeText(text: String): Boolean {
        if (text.isBlank()) return false
        if (text.contains("[Interface]", true)) return true
        val lower = text.lowercase()
        if (WIREGUARD_SCHEMES.any { lower.contains(it) }) return true
        if (OTHER_VPN_SCHEMES.any { lower.contains(it) }) return true
        return text.contains("://")
    }

    private fun looksLikeJson(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.startsWith("{")) return true
        if (!trimmed.startsWith("[")) return false
        if (trimmed.startsWith("[interface]", ignoreCase = true)) return false
        val next = trimmed.drop(1).trimStart().firstOrNull()
        return next == '{' || next == '"' || next == '['
    }

    private fun looksLikeHtml(text: String): Boolean {
        val head = text.take(600).lowercase()
        return head.startsWith("<!doctype") || head.startsWith("<html") ||
            (head.contains("<html") && head.contains("<head")) ||
            head.startsWith("<?xml") || head.contains("<body")
    }

    private fun regexTitle(html: String): String? =
        Regex("(?is)<title[^>]*>(.*?)</title>").find(html)?.groupValues?.getOrNull(1)
            ?.trim()?.takeIf { it.isNotEmpty() }?.take(80)

    private fun extractHttpUrls(text: String): List<String> =
        Regex("https?://[^\\s\"'<>]+").findAll(text)
            .map { it.value.trimEnd(',', '"', '\'', ')', ']') }
            .filter { it.length > 10 }
            .distinct()
            .toList()

    private fun stripComment(line: String): String {
        val hash = line.indexOf('#')
        val semi = line.indexOf(';')
        val cut = listOf(hash, semi).filter { it >= 0 }.minOrNull() ?: return line.trim()
        return line.substring(0, cut).trim()
    }

    private fun stripBom(text: String): String = text.removePrefix("\uFEFF")

    private fun normalizeEndpoint(raw: String): String? {
        var value = raw.trim()
        if (value.isEmpty()) return null
        value = value.removePrefix("[").let {
            if (raw.trim().startsWith("[")) "[$it" else it
        }
        // host:port / [v6]:port
        val host: String
        val portStr: String
        if (value.startsWith("[")) {
            val close = value.indexOf(']')
            if (close < 0) return null
            host = value.substring(1, close)
            portStr = value.substring(close + 1).removePrefix(":").trim()
        } else {
            val idx = value.lastIndexOf(':')
            if (idx <= 0) return null
            host = value.substring(0, idx)
            portStr = value.substring(idx + 1).trim()
        }
        if (host.isBlank()) return null
        val port = portStr.toIntOrNull()?.takeIf { it in 1..65535 } ?: return null
        return if (host.contains(':')) "[$host]:$port" else "$host:$port"
    }

    private fun parseQuery(query: String): Map<String, String> {
        if (query.isBlank()) return emptyMap()
        return query.split('&').mapNotNull { pair ->
            val eq = pair.indexOf('=')
            if (eq <= 0) null else pair.substring(0, eq) to pair.substring(eq + 1)
        }.toMap()
    }

    private fun urlDecode(value: String): String =
        runCatching { URLDecoder.decode(value, "UTF-8") }.getOrDefault(value)

    /**
     * Percent-decoding only. [URLDecoder] additionally maps '+' to a space, which silently
     * corrupts Base64 key material, so keys are decoded with this instead.
     */
    private fun percentDecode(value: String): String {
        val sb = StringBuilder(value.length)
        var i = 0
        while (i < value.length) {
            val c = value[i]
            if (c == '%' && i + 2 < value.length) {
                val code = value.substring(i + 1, i + 3).toIntOrNull(16)
                if (code != null) {
                    sb.append(code.toChar())
                    i += 3
                    continue
                }
            }
            sb.append(c)
            i++
        }
        return sb.toString()
    }

    private fun removeDuplicates(configs: List<SubscriptionConfig>): List<SubscriptionConfig> {
        val seen = mutableSetOf<String>()
        return configs.filter { config ->
            seen.add("${config.peerPublicKey}:${config.peerEndpoint}")
        }
    }

    private fun validateAndNormalizeUrl(url: String): String {
        val parsedUrl = try {
            URL(url)
        } catch (e: Exception) {
            throw SubscriptionParseException("لینک اشتراک نامعتبر است")
        }
        val protocol = parsedUrl.protocol.lowercase()
        if (protocol != "https" && protocol != "http") {
            throw SubscriptionParseException("فقط پروتکل‌های HTTPS و HTTP پشتیبانی می‌شوند")
        }
        val host = parsedUrl.host.lowercase()
        if (!allowPrivateHosts && isPrivateOrInternalHost(host)) {
            throw SubscriptionParseException("دسترسی به آدرس‌های داخلی مجاز نیست")
        }
        return url
    }

    private fun isPrivateOrInternalHost(host: String): Boolean {
        if (host == "localhost" || host == "127.0.0.1" || host == "::1") return true
        return try {
            val ip = java.net.InetAddress.getByName(host)
            ip.isLoopbackAddress || ip.isLinkLocalAddress || ip.isSiteLocalAddress ||
                ip.isAnyLocalAddress || isPrivateIPv4(ip.hostAddress)
        } catch (e: Exception) {
            host.startsWith("10.") ||
                (host.startsWith("172.") &&
                    host.substringAfter("172.").substringBefore(".").toIntOrNull()?.let { it in 16..31 } == true) ||
                host.startsWith("192.168.") ||
                host.startsWith("169.254.") ||
                host.startsWith("fc") || host.startsWith("fd")
        }
    }

    private fun isPrivateIPv4(ip: String): Boolean {
        val parts = ip.split(".").map { it.toIntOrNull() ?: return false }
        if (parts.size != 4) return false
        return when {
            parts[0] == 10 -> true
            parts[0] == 172 && parts[1] in 16..31 -> true
            parts[0] == 192 && parts[1] == 168 -> true
            parts[0] == 169 && parts[1] == 254 -> true
            parts[0] == 127 -> true
            else -> false
        }
    }

    // ------------------------------------------------------------------ networking

    private suspend fun fetch(url: String, userAgent: String = USER_AGENT): String =
        withContext(Dispatchers.IO) { fetchBlocking(url, userAgent) }

    private fun fetchBlocking(url: String, userAgent: String = USER_AGENT): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/plain, application/octet-stream, application/json, */*")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SubscriptionParseException(
                    "خطای HTTP ${response.code} از سرور اشتراک" +
                        if (response.code == 403) " (دسترسی رد شد؛ لینک را در مرورگر تست کنید)" else ""
                )
            }
            val body = response.body ?: throw SubscriptionParseException("پاسخ خالی از سرور")
            val declared = body.contentLength()
            if (declared > maxResponseSize) {
                throw SubscriptionParseException("حجم پاسخ سرور بیش از حد مجاز است")
            }
            val source = body.source()
            val buffer = Buffer()
            var total = 0L
            while (true) {
                val read = source.read(buffer, 8192)
                if (read == -1L) break
                total += read
                if (total > maxResponseSize) {
                    throw SubscriptionParseException("حجم پاسخ سرور بیش از حد مجاز است")
                }
            }
            return buffer.readString(StandardCharsets.UTF_8)
        }
    }

    private val BASE64_CHARS: Set<Char> =
        (('A'..'Z') + ('a'..'z') + ('0'..'9') + listOf('+', '/', '=', '-', '_')).toSet()
}

/**
 * Aggregated knowledge about what was found in a payload, so the failure message can say
 * exactly what the server returned instead of a generic "nothing found".
 */
private class ParseReport {
    val configs = mutableListOf<SubscriptionConfig>()
    var htmlLike: Boolean = false
    var detectedTitle: String? = null
    var urlListSize: Int = 0
    var linksSeen: Int = 0
    var preview: String? = null
    private val schemes = sortedSetOf<String>()

    fun merge(other: ParseReport) {
        configs.addAll(other.configs)
        htmlLike = htmlLike || other.htmlLike
        detectedTitle = detectedTitle ?: other.detectedTitle
        urlListSize += other.urlListSize
        linksSeen += other.linksSeen
        if (preview == null) preview = other.preview
        schemes.addAll(other.schemes)
    }

    fun rememberSchemes(text: String) {
        val lower = text.lowercase()
        val known = listOf(
            "vless://", "vmess://", "trojan://", "ss://", "ssr://",
            "hysteria2://", "hysteria://", "hy2://", "tuic://", "wireguard://", "wg://",
            "proxies:", "sing-box", "\"outbounds\""
        )
        known.forEach { if (lower.contains(it)) schemes.add(it) }
    }

    fun explain(): String = buildString {
        append("هیچ کانفیگ WireGuard در این اشتراک پیدا نشد")
        when {
            htmlLike -> {
                append(". پاسخ سرور یک صفحه وب بود")
                detectedTitle?.let { append(" («$it»)") }
                append("؛ معمولاً یعنی لینک منقضی شده، حساب غیرفعال است یا باید از داخل مرورگر وارد شوید")
            }
            schemes.isNotEmpty() -> {
                append(". فرمت شناسایی‌شده: ${schemes.joinToString(", ")}")
                append(" — این نوع اشتراک (Xray/V2Ray/Clash) با موتور WireGuard سازگار نیست")
            }
            linksSeen > 0 -> append(". $linksSeen لینک WireGuard پیدا شد ولی فیلدهایش کامل/معتبر نبود")
            urlListSize > 0 -> append(". فهرست لینک‌ها خالی بود یا هیچ‌کدام کانفیگ نداشتند")
            else -> {
                append(". محتوای پاسخ نه Base64 معتبر بود و نه متن پیکربندی")
                preview?.takeIf { it.isNotBlank() }?.let {
                    append("\nنمونهٔ پاسخ سرور: «$it»")
                }
            }
        }
    }
}

/**
 * Result of subscription parsing.
 */
data class SubscriptionParseResult(
    val configs: List<SubscriptionConfig>,
    val sourceUrl: String,
    val invalidConfigsCount: Int = 0,
    val duplicateConfigsCount: Int = 0
)

/**
 * Exception for subscription parsing errors.
 */
class SubscriptionParseException(
    message: String,
    cause: Throwable? = null
) : Exception(message, cause)
