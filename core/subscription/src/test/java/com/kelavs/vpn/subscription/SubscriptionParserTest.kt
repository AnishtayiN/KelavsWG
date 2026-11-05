/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.subscription

import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Base64

/**
 * Format coverage for the subscription parser. Every case here mirrors a format that a real
 * provider (Hiddify, Marzban, wg-portal, plain .conf hosting) actually serves.
 */
class SubscriptionParserTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var httpClient: OkHttpClient
    private lateinit var parser: SubscriptionParser

    private val privateKey = "yAnz5TF+lXXJte14tji9zl2my4neQ3Up4iQdW8g8h9E="
    private val peerKey = "xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg="

    private val configA = """
        [Interface]
        PrivateKey = $privateKey
        Address = 10.0.0.2/32
        DNS = 1.1.1.1
        MTU = 1280

        [Peer]
        PublicKey = $peerKey
        AllowedIPs = 0.0.0.0/0, ::/0
        Endpoint = de1.example.com:51820
        PersistentKeepalive = 25
    """.trimIndent()

    private val configB = """
        [Interface]
        PrivateKey = WAmD2bY1PT1p0Py7RXDg2Gk9dqQp8b0jVfO0ZKt4yVU=
        Address = 10.7.0.2/32

        [Peer]
        PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
        AllowedIPs = 0.0.0.0/0
        Endpoint = 198.51.100.7:51820
    """.trimIndent()

    @Before
    fun setUp() {
        mockServer = MockWebServer()
        mockServer.start()
        httpClient = OkHttpClient()
        parser = SubscriptionParser(
            httpClient = httpClient,
            maxResponseSize = 1024 * 1024,
            timeoutSeconds = 5,
            allowPrivateHosts = true
        )
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        httpClient.dispatcher.executorService.shutdown()
        httpClient.connectionPool.evictAll()
    }

    private fun serve(body: String) {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(body))
    }

    private fun url(path: String = "/sub") = mockServer.url(path).toString()

    // ---------------------------------------------------------------- plain formats

    @Test
    fun `parses a plain WireGuard configuration`() = runTest {
        serve(configA)
        val result = parser.parseSubscription(url())
        assertTrue(result.isSuccess)
        val config = result.getOrThrow().configs.single()
        assertEquals("de1.example.com:51820", config.peerEndpoint)
        assertEquals(1280, config.mtu)
        assertEquals(25, config.persistentKeepalive)
    }

    @Test
    fun `parses two configurations in one payload`() = runTest {
        serve("$configA\n\n$configB")
        val configs = parser.parseSubscription(url()).getOrThrow().configs
        assertEquals(2, configs.size)
    }

    @Test
    fun `handles CRLF line endings and inline comments`() = runTest {
        val messy = configA.replace("\n", "\r\n")
            .replace("MTU = 1280", "MTU = 1280   # keep small for Iran")
            .replace("PersistentKeepalive = 25", "PersistentKeepalive = 25 ; nat")
        serve(messy)
        val config = parser.parseSubscription(url()).getOrThrow().configs.single()
        assertEquals(1280, config.mtu)
        assertEquals(25, config.persistentKeepalive)
    }

    @Test
    fun `accepts an IPv6 endpoint`() = runTest {
        serve(configA.replace("de1.example.com:51820", "[2606:4700:110:8f81::1]:51820"))
        val config = parser.parseSubscription(url()).getOrThrow().configs.single()
        assertEquals("[2606:4700:110:8f81::1]:51820", config.peerEndpoint)
    }

    @Test
    fun `accepts a key without base64 padding`() = runTest {
        serve(configA.replace(privateKey, privateKey.trimEnd('=')))
        assertTrue(parser.parseSubscription(url()).isSuccess)
    }

    @Test
    fun `parses several peers under one interface`() = runTest {
        val twoPeers = """
            [Interface]
            PrivateKey = $privateKey
            Address = 10.0.0.2/32

            [Peer]
            PublicKey = $peerKey
            AllowedIPs = 0.0.0.0/0
            Endpoint = 198.51.100.7:51820

            [Peer]
            PublicKey = HIgo9xNzJMWLKASShiTqIybxZ0U3wGLiUeJ1PKf8ykw=
            AllowedIPs = 0.0.0.0/0
            Endpoint = 198.51.100.8:51820
        """.trimIndent()
        serve(twoPeers)
        assertEquals(2, parser.parseSubscription(url()).getOrThrow().configs.size)
    }

    // ---------------------------------------------------------------- base64

    @Test
    fun `parses standard base64 of two configs`() = runTest {
        serve(Base64.getEncoder().encodeToString("$configA\n$configB".toByteArray()))
        assertEquals(2, parser.parseSubscription(url()).getOrThrow().configs.size)
    }

    @Test
    fun `parses url-safe base64 without padding`() = runTest {
        val encoded = Base64.getUrlEncoder().withoutPadding()
            .encodeToString("$configA\n$configB".toByteArray())
        serve(encoded)
        assertEquals(2, parser.parseSubscription(url()).getOrThrow().configs.size)
    }

    @Test
    fun `parses base64 that wraps across lines`() = runTest {
        val encoded = Base64.getEncoder().encodeToString(configA.toByteArray())
        serve(encoded.chunked(64).joinToString("\n"))
        assertEquals(1, parser.parseSubscription(url()).getOrThrow().configs.size)
    }

    // ---------------------------------------------------------------- wg:// link schemes

    @Test
    fun `parses wg scheme carrying a base64 config`() = runTest {
        val payload = Base64.getEncoder().encodeToString(configA.toByteArray())
        serve("wg://$payload#🇩🇪 Germany 1")
        val config = parser.parseSubscription(url()).getOrThrow().configs.single()
        assertEquals("🇩🇪 Germany 1", config.name)
        assertEquals("de1.example.com:51820", config.peerEndpoint)
    }

    @Test
    fun `parses hiddify wireguard scheme with query parameters`() = runTest {
        val link = "wireguard://$privateKey@188.114.98.117:908" +
            "?address=172.16.0.2%2F32&publickey=$peerKey&mtu=1280&keepalive=25#WARP"
        serve(link)
        val config = parser.parseSubscription(url()).getOrThrow().configs.single()
        assertEquals("188.114.98.117:908", config.peerEndpoint)
        assertEquals("172.16.0.2/32", config.address)
        assertEquals(1280, config.mtu)
        assertEquals("WARP", config.name)
    }

    @Test
    fun `parses wg scheme with pk and peer_pk query`() = runTest {
        val link = "wg://162.159.192.1:894/?pk=$privateKey&peer_pk=$peerKey" +
            "&local_address=172.16.0.2/32&mtu=1420"
        serve(link)
        val config = parser.parseSubscription(url()).getOrThrow().configs.single()
        assertEquals("162.159.192.1:894", config.peerEndpoint)
        assertEquals(1420, config.mtu)
    }

    @Test
    fun `parses a mixed payload of links and plain configs`() = runTest {
        val payload = Base64.getEncoder().encodeToString(configB.toByteArray())
        serve("$configA\nwg://$payload#from-link")
        assertEquals(2, parser.parseSubscription(url()).getOrThrow().configs.size)
    }

    // ---------------------------------------------------------------- sing-box json

    @Test
    fun `parses sing-box json with a wireguard outbound`() = runTest {
        val json = """
            {"outbounds":[
              {"type":"wireguard","tag":"wg-de","server":"203.0.113.9","server_port":51820,
               "local_address":["10.66.0.2/32","fd00::2/128"],
               "private_key":"$privateKey","peer_public_key":"$peerKey","mtu":1420}
            ]}
        """.trimIndent()
        serve(json)
        val config = parser.parseSubscription(url()).getOrThrow().configs.single()
        assertEquals("203.0.113.9:51820", config.peerEndpoint)
        assertEquals("wg-de", config.name)
    }

    // ---------------------------------------------------------------- url lists, redirects

    @Test
    fun `resolves a list of URLs`() = runTest {
        // A dispatcher is required here: MockWebServer's enqueue queue is consumed in request
        // order, so path-based responses cannot be expressed with enqueue alone.
        val base = mockServer.url("/").toString().trimEnd('/')
        mockServer.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                return when (request.path) {
                    "/list" -> MockResponse().setResponseCode(200)
                        .setBody("$base/a.conf\n$base/b.conf")
                    "/a.conf" -> MockResponse().setResponseCode(200).setBody(configA)
                    "/b.conf" -> MockResponse().setResponseCode(200).setBody(configB)
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        assertEquals(2, parser.parseSubscription(url("/list")).getOrThrow().configs.size)
    }

    @Test
    fun `follows a redirect`() = runTest {
        mockServer.enqueue(
            MockResponse().setResponseCode(302).setHeader("Location", mockServer.url("/final"))
        )
        serve(configA)
        assertTrue(parser.parseSubscription(url("/start")).isSuccess)
    }

    // ---------------------------------------------------------------- actionable failures

    @Test
    fun `v2ray subscription explains that it is not WireGuard`() = runTest {
        val links = "vless://uuid@example.com:443?encryption=none#a\nvmess://eyJ2IjoiMiJ9"
        serve(Base64.getEncoder().encodeToString(links.toByteArray()))
        val error = parser.parseSubscription(url()).exceptionOrNull()
        assertTrue(error != null)
        val message = error!!.message.orEmpty()
        assertTrue(message, message.contains("V2Ray") || message.contains("vless"))
    }

    @Test
    fun `html answer reports an expired or login page`() = runTest {
        serve("<html><head><title>Login</title></head><body>expired</body></html>")
        val message = parser.parseSubscription(url()).exceptionOrNull()?.message.orEmpty()
        assertTrue(message, message.contains("صفحه وب"))
        assertTrue(message, message.contains("Login"))
    }

    @Test
    fun `clash yaml is reported as unsupported instead of empty`() = runTest {
        serve("proxies:\n  - name: a\n    type: ss\n")
        val message = parser.parseSubscription(url()).exceptionOrNull()?.message.orEmpty()
        assertTrue(message, message.contains("proxies:") || message.contains("سازگار نیست"))
    }

    @Test
    fun `http error is surfaced`() = runTest {
        // 403 triggers one browser-User-Agent retry, so both attempts must be answered.
        mockServer.enqueue(MockResponse().setResponseCode(403).setBody("nope"))
        mockServer.enqueue(MockResponse().setResponseCode(403).setBody("nope"))
        val message = parser.parseSubscription(url()).exceptionOrNull()?.message.orEmpty()
        assertTrue(message, message.contains("403"))
    }

    @Test
    fun `retries with a browser user agent when the panel returns 403`() = runTest {
        mockServer.dispatcher = object : okhttp3.mockwebserver.Dispatcher() {
            override fun dispatch(request: okhttp3.mockwebserver.RecordedRequest): MockResponse {
                val ua = request.getHeader("User-Agent").orEmpty()
                return if (ua.contains("Mozilla")) {
                    MockResponse().setResponseCode(200).setBody(configA)
                } else {
                    MockResponse().setResponseCode(403)
                }
            }
        }
        val result = parser.parseSubscription(url())
        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().configs.size)
    }

    // ---------------------------------------------------------------- pasted text

    @Test
    fun `parses pasted configuration text without any network`() = runTest {
        val result = parser.parseText(configA)
        assertTrue(result.isSuccess)
        assertEquals("de1.example.com:51820", result.getOrThrow().configs.single().peerEndpoint)
    }

    @Test
    fun `rejects pasted text that is not a wireguard config`() = runTest {
        val result = parser.parseText("hello world, this is not a config at all")
        assertTrue(result.isFailure)
    }

    // ---------------------------------------------------------------- guards

    @Test
    fun `rejects private hosts when the SSRF guard is enabled`() = runTest {
        val strict = SubscriptionParser(httpClient = httpClient, timeoutSeconds = 5)
        assertTrue(strict.parseSubscription(url()).isFailure)
    }

    @Test
    fun `rejects unsupported protocols and malformed urls`() = runTest {
        assertTrue(parser.parseSubscription("ftp://example.com/sub").isFailure)
        assertTrue(parser.parseSubscription("not a url").isFailure)
    }
}
