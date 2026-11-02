package com.kelavs.vpn.subscription

import android.util.Base64
import com.kelavs.vpn.network.SafeHttpClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/**
 * Parses various subscription formats and extracts WireGuard configurations.
 * Supports:
 * 1. Direct .conf file
 * 2. Plain text WireGuard config
 * 3. Base64 encoded WireGuard config
 * 4. Text subscription with multiple WireGuard configs
 * 5. URLs listing WireGuard config URLs
 * 6. Direct config download links
 */
class SubscriptionParser(
    private val httpClient: OkHttpClient = SafeHttpClient.create(),
    private val maxResponseSize: Long = 10 * 1024 * 1024, // 10 MB
    private val timeoutSeconds: Int = 30
) {

    private val wireGuardConfigPattern = Pattern.compile(
        "\\[Interface\\].*?\\[Peer\\].*?(?=\\[Interface\\]|\\z)",
        Pattern.DOTALL
    )

    private val keyValuePattern = Pattern.compile("^\\s*(\\w+)\\s*=\\s*(.+?)\\s*$", Pattern.MULTILINE)

    /**
     * Fetches and parses a subscription from the given URL.
     * Follows safe HTTPS redirects, validates content, extracts WireGuard configs.
     */
    suspend fun parseSubscription(url: String): Result<SubscriptionParseResult> {
        return try {
            // Validate URL
            val validatedUrl = validateAndNormalizeUrl(url)
            
            // Fetch content with redirect following
            val content = fetchWithRedirects(validatedUrl)
            
            // Parse based on content type
            val configs = parseContent(content, validatedUrl)
            
            if (configs.isEmpty()) {
                Result.failure(SubscriptionParseException("هیچ کانفیگ WireGuard معتبری یافت نشد"))
            } else {
                // Remove duplicates
                val uniqueConfigs = removeDuplicates(configs)
                
                // Validate each config
                val validConfigs = uniqueConfigs.filter { it.isValid() }
                val invalidCount = uniqueConfigs.size - validConfigs.size
                
                if (validConfigs.isEmpty()) {
                    Result.failure(SubscriptionParseException("هیچ کانفیگ معتبری پس از اعتبارسنجی باقی نمانید"))
                } else {
                    Result.success(SubscriptionParseResult(
                        configs = validConfigs,
                        sourceUrl = validatedUrl,
                        invalidConfigsCount = invalidCount,
                        duplicateConfigsCount = configs.size - uniqueConfigs.size
                    ))
                }
            }
        } catch (e: SubscriptionParseException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(SubscriptionParseException("خطای غیرمنتظره: ${e.message}", e))
        }
    }

    /**
     * Validates URL structure and ensures HTTPS (with optional HTTP fallback).
     */
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

        if (protocol == "http") {
            // HTTP is allowed but will show security warning in UI
            // For now we allow it but log warning
        }

        // Prevent SSRF - block private/internal IPs
        val host = parsedUrl.host.lowercase()
        if (isPrivateOrInternalHost(host)) {
            throw SubscriptionParseException("دسترسی به آدرس‌های داخلی مجاز نیست")
        }

        return url
    }

    private fun isPrivateOrInternalHost(host: String): Boolean {
        // Block localhost
        if (host == "localhost" || host == "127.0.0.1" || host == "::1") return true
        
        // Block private IP ranges
        // 10.0.0.0/8, 172.16.0.0/12, 192.168.0.0/16
        // 169.254.0.0/16 (link-local)
        // fc00::/7 (ULA)
        return try {
            val ip = java.net.InetAddress.getByName(host)
            ip.isLoopbackAddress || ip.isLinkLocalAddress || ip.isSiteLocalAddress ||
            ip.isAnyLocalAddress || isPrivateIPv4(ip.hostAddress)
        } catch (e: Exception) {
            // If we can't resolve, check hostname patterns
            host.startsWith("10.") ||
            host.startsWith("172.") && host.substringAfter("172.").substringBefore(".").toIntOrNull()?.inRange(16, 31) == true ||
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
            else -> false
        }
    }

    /**
     * Fetches content following safe HTTPS redirects.
     */
    private suspend fun fetchWithRedirects(url: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "KelavsVPN/1.0 (Android; Subscription Parser)")
            .header("Accept", "text/plain, application/octet-stream, */*")
            .build()

        httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                throw SubscriptionParseException("خطای HTTP: ${response.code} ${response.message}")
            }

            val body = response.body ?: throw SubscriptionParseException("پاسخ خالی از سرور")
            
            // Limit response size
            val contentLength = body.contentLength()
            if (contentLength > 0 && contentLength > maxResponseSize) {
                throw SubscriptionParseException("حجم پاسخ سرور از حد مجاز بیشتر است (${maxResponseSize / 1024 / 1024} MB)")
            }

            val source = body.source()
            val buffer = okio.Buffer()
            var totalRead: Long = 0
            while (true) {
                val read = source.read(buffer, 8192)
                if (read == -1L) break
                totalRead += read
                if (totalRead > maxResponseSize) {
                    throw SubscriptionParseException("حجم پاسخ سرور از حد مجاز بیشتر است")
                }
            }
            
            buffer.readUtf8()
        }
    }

    /**
     * Parses content based on detected format.
     */
    private fun parseContent(content: String, sourceUrl: String): List<SubscriptionConfig> {
        val trimmed = content.trim()
        
        // Check if it's Base64 encoded
        if (looksLikeBase64(trimmed)) {
            return parseBase64Content(trimmed, sourceUrl)
        }
        
        // Check if it's a list of URLs (one per line)
        if (isUrlList(trimmed)) {
            return parseUrlList(trimmed, sourceUrl)
        }
        
        // Check if it's a direct WireGuard config
        if (trimmed.startsWith("[Interface]") || trimmed.contains("[Interface]")) {
            return parseWireGuardConfigs(trimmed, sourceUrl)
        }
        
        // Try to parse as WireGuard config anyway
        return parseWireGuardConfigs(trimmed, sourceUrl)
    }

    private fun looksLikeBase64(str: String): Boolean {
        // Base64 regex: only alphanumeric, +, /, =, and whitespace
        return str.length % 4 == 0 && 
               Pattern.matches("^[A-Za-z0-9+/=\\s]+$", str) &&
               str.length > 100 // Reasonable minimum size for a config
    }

    private fun isUrlList(content: String): Boolean {
        val lines = content.lines().filter { it.isNotBlank() }
        return lines.all { it.startsWith("http://") || it.startsWith("https://") }
    }

    private fun parseBase64Content(base64: String, sourceUrl: String): List<SubscriptionConfig> {
        val cleaned = base64.replace("\\s".toRegex(), "")
        val decoded = try {
            String(Base64.decode(cleaned, Base64.NO_WRAP), "UTF-8")
        } catch (e: Exception) {
            throw SubscriptionParseException("خطا در استخراج Base64: ${e.message}")
        }
        return parseContent(decoded, sourceUrl)
    }

    private fun parseUrlList(content: String, sourceUrl: String): List<SubscriptionConfig> {
        val urls = content.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() && (it.startsWith("http://") || it.startsWith("https://")) }
            .distinct()
            .take(50) // Limit to prevent abuse
        
        val allConfigs = mutableListOf<SubscriptionConfig>()
        
        for (url in urls) {
            try {
                val subContent = fetchWithRedirectsBlocking(url)
                val configs = parseContent(subContent, url)
                allConfigs.addAll(configs)
            } catch (e: Exception) {
                // Log but continue with other URLs
                // In production, use proper logging
            }
        }
        
        return allConfigs
    }

    private fun fetchWithRedirectsBlocking(url: String): String {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "KelavsVPN/1.0")
            .build()
        
        return httpClient.newCall(request).execute().use { response ->
            if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
            response.body?.source()?.readUtf8() ?: throw IOException("Empty response")
        }
    }

    private fun parseWireGuardConfigs(content: String, sourceUrl: String): List<SubscriptionConfig> {
        val configs = mutableListOf<SubscriptionConfig>()
        
        // Normalize line endings
        val normalized = content.replace("\r\n", "\n").replace("\r", "\n")
        
        // Find all [Interface]...[Peer] blocks
        val matcher = wireGuardConfigPattern.matcher(normalized)
        var configIndex = 0
        
        while (matcher.find()) {
            val configText = matcher.group()
            val config = parseSingleConfig(configText, sourceUrl, configIndex++)
            config?.let { configs.add(it) }
        }
        
        // If no [Interface] found but content looks like a config, try parsing whole thing
        if (configs.isEmpty() && normalized.contains("PrivateKey") && normalized.contains("PublicKey")) {
            parseSingleConfig(normalized, sourceUrl, 0)?.let { configs.add(it) }
        }
        
        return configs
    }

    private fun parseSingleConfig(configText: String, sourceUrl: String, index: Int): SubscriptionConfig? {
        val map = mutableMapOf<String, String>()
        
        // Extract key-value pairs from both [Interface] and [Peer] sections
        val matcher = keyValuePattern.matcher(configText)
        while (matcher.find()) {
            val key = matcher.group(1)
            val value = matcher.group(2).trim()
            map[key] = value
        }
        
        // Required fields
        val privateKey = map["PrivateKey"] ?: return null
        val address = map["Address"] ?: return null
        val peerPublicKey = map["PublicKey"] ?: return null
        val peerAllowedIPs = map["AllowedIPs"] ?: return null
        val peerEndpoint = map["Endpoint"] ?: return null
        
        // Optional fields
        val dns = map["DNS"]
        val mtu = map["MTU"]?.toIntOrNull()
        val persistentKeepalive = map["PersistentKeepalive"]?.toIntOrNull()
        val presharedKey = map["PresharedKey"]
        
        // Generate name from endpoint or index
        val name = map["#Name"] ?: map["Name"] ?: "Server ${index + 1} - ${peerEndpoint.split(":")[0]}"
        
        return SubscriptionConfig(
            name = name,
            privateKey = privateKey,
            address = address,
            dns = dns,
            mtu = mtu,
            persistentKeepalive = persistentKeepalive,
            peerPublicKey = peerPublicKey,
            peerAllowedIPs = peerAllowedIPs,
            peerEndpoint = peerEndpoint,
            peerPresharedKey = presharedKey,
            originalConfig = configText.trim()
        )
    }

    private fun removeDuplicates(configs: List<SubscriptionConfig>): List<SubscriptionConfig> {
        val seen = mutableSetOf<String>()
        return configs.filter { config ->
            val key = "${config.peerPublicKey}:${config.peerEndpoint}:${config.peerAllowedIPs}"
            seen.add(key)
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