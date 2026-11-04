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
 * Tests the real subscription parser against a local MockWebServer.
 *
 * `allowPrivateHosts = true` is required because MockWebServer listens on 127.0.0.1, which the
 * production SSRF guard rejects by default.
 */
class SubscriptionParserTest {

    private lateinit var mockServer: MockWebServer
    private lateinit var httpClient: OkHttpClient
    private lateinit var parser: SubscriptionParser

    private val singleConfig = """
        [Interface]
        PrivateKey = yAnz5TF+lXXJte14tji9zl2my4neQ3Up4iQdW8g8h9E=
        Address = 10.0.0.2/32
        DNS = 1.1.1.1
        MTU = 1280

        [Peer]
        PublicKey = xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg=
        AllowedIPs = 0.0.0.0/0, ::/0
        Endpoint = example.com:51820
        PersistentKeepalive = 25
    """.trimIndent()

    private val secondConfig = """
        [Interface]
        PrivateKey = WAmD2bY1PT1p0Py7RXDg2Gk9dqQp8b0jVfO0ZKt4yVU=
        Address = 10.7.0.2/32
        DNS = 8.8.8.8

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

    @Test
    fun `parses a plain WireGuard configuration`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(singleConfig))

        val result = parser.parseSubscription(mockServer.url("/sub").toString())

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(1, parsed.configs.size)
        val config = parsed.configs.first()
        assertEquals("example.com:51820", config.peerEndpoint)
        assertEquals("xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg=", config.peerPublicKey)
        assertEquals(1280, config.mtu)
        assertEquals(25, config.persistentKeepalive)
        assertTrue(config.isValid())
    }

    @Test
    fun `parses a base64 encoded subscription`() = runTest {
        val encoded = Base64.getEncoder().encodeToString(
            (singleConfig + "\n" + secondConfig).toByteArray()
        )
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(encoded))

        val result = parser.parseSubscription(mockServer.url("/sub").toString())

        assertTrue(result.isSuccess)
        assertEquals(2, result.getOrThrow().configs.size)
    }

    @Test
    fun `parses multiple configurations in one payload`() = runTest {
        mockServer.enqueue(
            MockResponse().setResponseCode(200).setBody("$singleConfig\n\n$secondConfig")
        )

        val result = parser.parseSubscription(mockServer.url("/sub").toString())

        assertTrue(result.isSuccess)
        val parsed = result.getOrThrow()
        assertEquals(2, parsed.configs.size)
        assertEquals(
            setOf("example.com:51820", "198.51.100.7:51820"),
            parsed.configs.map { it.peerEndpoint }.toSet()
        )
    }

    @Test
    fun `follows a redirect to the real configuration`() = runTest {
        mockServer.enqueue(
            MockResponse().setResponseCode(302).setHeader("Location", mockServer.url("/final"))
        )
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody(singleConfig))

        val result = parser.parseSubscription(mockServer.url("/start").toString())

        assertTrue(result.isSuccess)
        assertEquals(1, result.getOrThrow().configs.size)
    }

    @Test
    fun `fails on unsupported protocol`() = runTest {
        val result = parser.parseSubscription("ftp://example.com/sub")

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails on malformed url`() = runTest {
        val result = parser.parseSubscription("not a url")

        assertTrue(result.isFailure)
    }

    @Test
    fun `rejects private hosts when SSRF guard is enabled`() = runTest {
        val strictParser = SubscriptionParser(
            httpClient = httpClient,
            timeoutSeconds = 5,
            allowPrivateHosts = false
        )

        val result = strictParser.parseSubscription(mockServer.url("/sub").toString())

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails when payload contains no WireGuard config`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(200).setBody("<html>nothing here</html>"))

        val result = parser.parseSubscription(mockServer.url("/sub").toString())

        assertTrue(result.isFailure)
    }

    @Test
    fun `fails on server error`() = runTest {
        mockServer.enqueue(MockResponse().setResponseCode(500).setBody("boom"))

        val result = parser.parseSubscription(mockServer.url("/sub").toString())

        assertTrue(result.isFailure)
    }
}
