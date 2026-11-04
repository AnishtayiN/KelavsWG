/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.network

import okhttp3.ConnectionPool
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.buffer
import java.security.KeyStore
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

/**
 * Factory for the app's HTTP clients.
 *
 * Security decisions:
 *  - Only the platform trust store is used; there is no "trust all" manager anywhere in the app.
 *  - Hostname verification stays at OkHttp's default (strict) implementation.
 *  - Logging is disabled at body level so that subscription URLs and keys never reach logcat.
 */
object SafeHttpClient {

    private const val CONNECT_TIMEOUT_SECONDS = 15L
    private const val READ_TIMEOUT_SECONDS = 30L
    private const val WRITE_TIMEOUT_SECONDS = 30L
    private const val MAX_IDLE_CONNECTIONS = 5
    private const val KEEP_ALIVE_DURATION_MINUTES = 5L

    private val trustManager: X509TrustManager by lazy { createSystemTrustManager() }
    private val sslSocketFactory by lazy {
        SSLContext.getInstance("TLS").apply {
            init(null, arrayOf(trustManager), null)
        }.socketFactory
    }

    /** Client used to fetch subscriptions. */
    fun create(): OkHttpClient = baseBuilder()
        .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .readTimeout(READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .writeTimeout(WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(MAX_IDLE_CONNECTIONS, KEEP_ALIVE_DURATION_MINUTES, TimeUnit.MINUTES))
        .dispatcher(Dispatcher().apply {
            maxRequests = 10
            maxRequestsPerHost = 3
        })
        .addNetworkInterceptor(createLoggingInterceptor())
        .build()

    /** Lightweight client used to talk to the GitHub releases API. */
    fun createUpdateClient(): OkHttpClient = baseBuilder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(20, TimeUnit.SECONDS)
        .connectionPool(ConnectionPool(3, 2, TimeUnit.MINUTES))
        .build()

    /** Client used to download the APK, optionally reporting progress. */
    fun createDownloadClient(progressListener: ((Long, Long) -> Unit)? = null): OkHttpClient =
        baseBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectionPool(ConnectionPool(2, 5, TimeUnit.MINUTES))
            .retryOnConnectionFailure(true)
            .addNetworkInterceptor { chain ->
                val response = chain.proceed(chain.request())
                val body = response.body
                if (progressListener == null || body == null) {
                    response
                } else {
                    response.newBuilder()
                        .body(ProgressResponseBody(body, progressListener))
                        .build()
                }
            }
            .build()

    private fun baseBuilder(): OkHttpClient.Builder = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .retryOnConnectionFailure(false)
        .sslSocketFactory(sslSocketFactory, trustManager)

    private fun createSystemTrustManager(): X509TrustManager {
        val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
        factory.init(null as KeyStore?)
        return factory.trustManagers.filterIsInstance<X509TrustManager>().firstOrNull()
            ?: throw IllegalStateException("No X509TrustManager available")
    }

    private fun createLoggingInterceptor(): HttpLoggingInterceptor =
        HttpLoggingInterceptor().apply {
            // Never log bodies: they may contain private keys.
            level = HttpLoggingInterceptor.Level.NONE
        }
}

/**
 * Response body wrapper that reports download progress.
 */
class ProgressResponseBody(
    private val responseBody: ResponseBody,
    private val progressListener: (bytesRead: Long, contentLength: Long) -> Unit
) : ResponseBody() {

    @Volatile
    private var bytesRead: Long = 0L

    private val forwardingSource: ForwardingSource = object : ForwardingSource(responseBody.source()) {
        override fun read(sink: Buffer, byteCount: Long): Long {
            val read = super.read(sink, byteCount)
            if (read > 0) {
                bytesRead += read
                progressListener(bytesRead, responseBody.contentLength())
            }
            return read
        }
    }

    override fun contentType() = responseBody.contentType()

    override fun contentLength(): Long = responseBody.contentLength()

    override fun source(): BufferedSource = forwardingSource.buffer()
}
