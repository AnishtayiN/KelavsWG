package com.kelavs.vpn.subscription

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class SubscriptionParserTest {

    private val testDispatcher = TestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var mockServer: MockWebServer
    private lateinit var parser: SubscriptionParser

    @Before
    fun setup() {
        mockServer = MockWebServer()
        mockServer.start()
        parser = SubscriptionParser(
            httpClient = mockServer.client,
            maxResponseSize = 1024 * 1024,
            timeoutSeconds = 5
        )
    }

    @After
    fun tearDown() {
        mockServer.shutdown()
        testDispatcher.cleanupTestCoroutines()
    }

    @Test
    fun `parse valid wireguard config`() = testScope.runTest {
        val config = """
            [Interface]
            PrivateKey = yAnz5TF+lXXJte14tji9zl2my4neQ3Up4iQdW8g8h9E=
            Address = 10.0.0.2/24
            DNS = 1.1.1.1

            [Peer]
            PublicKey = xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg=
            AllowedIPs = 0.0.0.0/0
            Endpoint = example.com:51820
            PersistentKeepalive = 25
        """.trimIndent()

        mockServer.enqueue(MockResponse().setBody(config).setResponseCode(200))

        val result = parser.parseSubscription(mockServer.url("/").toString())
        
        assertTrue(result is SubscriptionParseResult.Success)
        val parseResult = (result as SubscriptionParseResult.Success).value
        assertEquals(1, parseResult.configs.size)
        assertTrue(parseResult.configs[0].isValid())
    }

    @Test
    fun `parse multiple wireguard configs`() = testScope.runTest {
        val config = """
            [Interface]
            PrivateKey = yAnz5TF+lXXJte14tji9zl2my4neQ3Up4iQdW8g8h9E=
            Address = 10.0.0.2/24
            [Peer]
            PublicKey = xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg=
            AllowedIPs = 0.0.0.0/0
            Endpoint = server1.example.com:51820

            [Interface]
            PrivateKey = yAnz5TF+lXXJte14tji9zl2my4neQ3Up4iQdW8g8h9E=
            Address = 10.0.0.3/24
            [Peer]
            PublicKey = xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg=
            AllowedIPs = 0.0.0.0/0
            Endpoint = server2.example.com:51820
        """.trimIndent()

        mockServer.enqueue(MockResponse().setBody(config).setResponseCode(200))

        val result = parser.parseSubscription(mockServer.url("/").toString())
        
        assertTrue(result is SubscriptionParseResult.Success)
        val parseResult = (result as SubscriptionParseResult.Success).value
        assertEquals(2, parseResult.configs.size)
    }

    @Test
    fun `parse base64 encoded config`() = testScope.runTest {
        val config = """
            [Interface]
            PrivateKey = yAnz5TF+lXXJte14tji9zl2my4neQ3Up4iQdW8g8h9E=
            Address = 10.0.0.2/24
            [Peer]
            PublicKey = xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg=
            AllowedIPs = 0.0.0.0/0
            Endpoint = example.com:51820
        """.trimIndent()
        
        val base64 = android.util.Base64.encodeToString(config.toByteArray(), android.util.Base64.NO_WRAP)

        mockServer.enqueue(MockResponse().setBody(base64).setResponseCode(200))

        val result = parser.parseSubscription(mockServer.url("/").toString())
        
        assertTrue(result is SubscriptionParseResult.Success)
        val parseResult = (result as SubscriptionParseResult.Success).value
        assertEquals(1, parseResult.configs.size)
    }

    @Test
    fun `reject invalid url`() = testScope.runTest {
        val result = parser.parseSubscription("not-a-url")
        assertTrue(result is SubscriptionParseResult.Failure)
    }

    @Test
    fun `reject private ip url`() = testScope.runTest {
        val result = parser.parseSubscription("http://192.168.1.1/config")
        assertTrue(result is SubscriptionParseResult.Failure)
    }

    @Test
    fun `reject localhost url`() = testScope.runTest {
        val result = parser.parseSubscription("http://localhost/config")
        assertTrue(result is SubscriptionParseResult.Failure)
    }

    @Test
    fun `handle http error`() = testScope.runTest {
        mockServer.enqueue(MockResponse().setResponseCode(404))

        val result = parser.parseSubscription(mockServer.url("/").toString())
        
        assertTrue(result is SubscriptionParseResult.Failure)
    }

    @Test
    fun `handle empty response`() = testScope.runTest {
        mockServer.enqueue(MockResponse().setBody("").setResponseCode(200))

        val result = parser.parseSubscription(mockServer.url("/").toString())
        
        assertTrue(result is SubscriptionParseResult.Failure)
    }

    @Test
    fun `remove duplicate configs`() = testScope.runTest {
        val config = """
            [Interface]
            PrivateKey = yAnz5TF+lXXJte14tji9zl2my4neQ3Up4iQdW8g8h9E=
            Address = 10.0.0.2/24
            [Peer]
            PublicKey = xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg=
            AllowedIPs = 0.0.0.0/0
            Endpoint = example.com:51820

            [Interface]
            PrivateKey = yAnz5TF+lXXJte14tji9zl2my4neQ3Up4iQdW8g8h9E=
            Address = 10.0.0.2/24
            [Peer]
            PublicKey = xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg=
            AllowedIPs = 0.0.0.0/0
            Endpoint = example.com:51820
        """.trimIndent()

        mockServer.enqueue(MockResponse().setBody(config).setResponseCode(200))

        val result = parser.parseSubscription(mockServer.url("/").toString())
        
        assertTrue(result is SubscriptionParseResult.Success)
        val parseResult = (result as SubscriptionParseResult.Success).value
        assertEquals(1, parseResult.configs.size)
        assertEquals(1, parseResult.duplicateConfigsCount)
    }

    @Test
    fun `reject config missing required fields`() = testScope.runTest {
        val config = """
            [Interface]
            PrivateKey = yAnz5TF+lXXJte14tji9zl2my4neQ3Up4iQdW8g8h9E=
            [Peer]
            PublicKey = xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg=
        """.trimIndent()

        mockServer.enqueue(MockResponse().setBody(config).setResponseCode(200))

        val result = parser.parseSubscription(mockServer.url("/").toString())
        
        assertTrue(result is SubscriptionParseResult.Failure)
    }

    @Test
    fun `follow redirects`() = testScope.runTest {
        val config = """
            [Interface]
            PrivateKey = yAnz5TF+lXXJte14tji9zl2my4neQ3Up4iQdW8g8h9E=
            Address = 10.0.0.2/24
            [Peer]
            PublicKey = xTIBA5rboUvnH4htodjb6e697QjLERt1NAB4mZqp8Dg=
            AllowedIPs = 0.0.0.0/0
            Endpoint = example.com:51820
        """.trimIndent()

        mockServer.enqueue(MockResponse().setResponseCode(302).setHeader("Location", mockServer.url("/config").toString()))
        mockServer.enqueue(MockResponse().setBody(config).setResponseCode(200))

        val result = parser.parseSubscription(mockServer.url("/").toString())
        
        assertTrue(result is SubscriptionParseResult.Success)
    }
}