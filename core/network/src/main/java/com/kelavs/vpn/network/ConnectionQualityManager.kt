package com.kelavs.vpn.network

import com.kelavs.vpn.storage.ServerConfigEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

/**
 * Manages server quality metrics and automatic server selection.
 * Prioritizes real WireGuard handshake success over simple ping/port checks.
 */
class ConnectionQualityManager(
    private val scope: CoroutineScope,
    private val onServerSelected: (ServerConfigEntity) -> Unit
) {

    private val serverMetrics = ConcurrentHashMap<String, ServerMetrics>()
    private val currentServer = AtomicReference<ServerConfigEntity?>(null)
    private val isAutoSelectionEnabled = AtomicBoolean(true)
    private val lastSelectionTime = AtomicLong(0)
    private val selectionCooldownMs = 30_000 // 30 seconds minimum between auto-switches

    // Configuration
    private val maxFailureCount = 3
    private val handshakeTimeoutMs = 10_000
    private val minHandshakeIntervalMs = 60_000 // Expect handshake at least every minute

    /**
     * Records a successful WireGuard handshake.
     * This is the PRIMARY metric for server quality.
     */
    fun recordHandshakeSuccess(serverPublicKey: String, latencyMs: Long? = null) {
        val metrics = serverMetrics.computeIfAbsent(serverPublicKey) { ServerMetrics() }
        metrics.recordSuccess(latencyMs)
        
        // If this is current server, update its quality
        currentServer.get()?.let { current ->
            if (current.publicKey == serverPublicKey) {
                // Current server is working well
            }
        }
    }

    /**
     * Records a handshake failure or timeout.
     */
    fun recordHandshakeFailure(serverPublicKey: String) {
        val metrics = serverMetrics.computeIfAbsent(serverPublicKey) { ServerMetrics() }
        metrics.recordFailure()
        
        // Check if we should switch servers
        if (isAutoSelectionEnabled.get()) {
            evaluateServerSwitch()
        }
    }

    /**
     * Records a connection attempt result.
     */
    fun recordConnectionAttempt(serverPublicKey: String, success: Boolean, latencyMs: Long? = null) {
        val metrics = serverMetrics.computeIfAbsent(serverPublicKey) { ServerMetrics() }
        if (success) {
            metrics.recordSuccess(latencyMs)
        } else {
            metrics.recordFailure()
        }
    }

    /**
     * Evaluates if we should switch to a better server.
     */
    private fun evaluateServerSwitch() {
        val now = System.currentTimeMillis()
        if (now - lastSelectionTime.get() < selectionCooldownMs) return
        
        val current = currentServer.get() ?: return
        val currentMetrics = serverMetrics[current.publicKey] ?: return
        
        // Don't switch if current server is healthy
        if (currentMetrics.isHealthy) return
        
        // Find best alternative
        val candidates = serverMetrics.entries
            .filter { (key, metrics) -> 
                key != current.publicKey && 
                metrics.isHealthy && 
                metrics.failureCount < maxFailureCount
            }
            .toList()
        
        if (candidates.isEmpty()) return
        
        // Sort by quality score (handshake success rate, latency, recency)
        val best = candidates.maxByOrNull { (_, metrics) -> metrics.qualityScore }
            ?: return
        
        // Only switch if significantly better
        if (best.value.qualityScore > currentMetrics.qualityScore * 1.5) {
            lastSelectionTime.set(now)
            // Find the server entity and select it
            // This would need access to server list - callback handles it
        }
    }

    /**
     * Manually selects a server.
     */
    fun selectServer(server: ServerConfigEntity) {
        currentServer.set(server)
        lastSelectionTime.set(System.currentTimeMillis())
        onServerSelected(server)
    }

    /**
     * Gets the current server.
     */
    fun getCurrentServer(): ServerConfigEntity? = currentServer.get()

    /**
     * Gets quality metrics for a server.
     */
    fun getMetrics(serverPublicKey: String): ServerMetrics? = serverMetrics[serverPublicKey]

    /**
     * Gets all server metrics.
     */
    fun getAllMetrics(): Map<String, ServerMetrics> = serverMetrics.toMap()

    /**
     * Enables/disables automatic server selection.
     */
    fun setAutoSelectionEnabled(enabled: Boolean) {
        isAutoSelectionEnabled.set(enabled)
    }

    /**
     * Checks if auto selection is enabled.
     */
    fun isAutoSelectionEnabled(): Boolean = isAutoSelectionEnabled.get()

    /**
     * Resets metrics for a server (e.g., after config update).
     */
    fun resetMetrics(serverPublicKey: String) {
        serverMetrics.remove(serverPublicKey)
    }

    /**
     * Updates metrics from tunnel statistics.
     */
    fun updateFromTunnelStats(serverPublicKey: String, stats: TunnelStats) {
        val metrics = serverMetrics.computeIfAbsent(serverPublicKey) { ServerMetrics() }
        metrics.updateFromStats(stats)
    }
}

/**
 * Metrics for a single server.
 */
data class ServerMetrics(
    var handshakeSuccessCount: Long = 0,
    var handshakeFailureCount: Long = 0,
    var totalLatencyMs: Long = 0,
    var latencySamples: Long = 0,
    var lastHandshakeTime: Long = 0,
    var lastFailureTime: Long = 0,
    var connectionAttempts: Long = 0,
    var successfulConnections: Long = 0,
    var bytesReceived: Long = 0,
    var bytesSent: Long = 0,
    var lastUpdateTime: Long = System.currentTimeMillis()
) {

    val successRate: Double
        get() = if (handshakeSuccessCount + handshakeFailureCount == 0) 1.0
        else handshakeSuccessCount.toDouble() / (handshakeSuccessCount + handshakeFailureCount)

    val avgLatencyMs: Long
        get() = if (latencySamples == 0) 0 else totalLatencyMs / latencySamples

    val failureCount: Int = handshakeFailureCount.toInt()

    val isHealthy: Boolean
        get() = successRate > 0.7 && failureCount < 5 && 
                (System.currentTimeMillis() - lastHandshakeTime < 300_000 || lastHandshakeTime == 0)

    val qualityScore: Double
        get() {
            if (!isHealthy) return 0.0
            val latencyFactor = if (avgLatencyMs > 0) 1000.0 / avgLatencyMs else 1.0
            val recencyFactor = if (lastHandshakeTime > 0) {
                val ageMinutes = (System.currentTimeMillis() - lastHandshakeTime) / 60_000
                max(0.1, 1.0 - ageMinutes / 60.0) // Decay over 1 hour
            } else 0.5
            return successRate * 100 + latencyFactor * 10 + recencyFactor * 5
        }

    fun recordSuccess(latencyMs: Long?) {
        handshakeSuccessCount++
        lastHandshakeTime = System.currentTimeMillis()
        latencyMs?.let {
            totalLatencyMs += it
            latencySamples++
        }
        lastUpdateTime = System.currentTimeMillis()
    }

    fun recordFailure() {
        handshakeFailureCount++
        lastFailureTime = System.currentTimeMillis()
        lastUpdateTime = System.currentTimeMillis()
    }

    fun updateFromStats(stats: TunnelStats) {
        bytesReceived = stats.rxBytes
        bytesSent = stats.txBytes
        if (stats.lastHandshakeTime > 0) {
            lastHandshakeTime = stats.lastHandshakeTime
        }
        lastUpdateTime = System.currentTimeMillis()
    }
}

/**
 * Tunnel statistics from WireGuard.
 */
data class TunnelStats(
    val rxBytes: Long,
    val txBytes: Long,
    val lastHandshakeTime: Long
)