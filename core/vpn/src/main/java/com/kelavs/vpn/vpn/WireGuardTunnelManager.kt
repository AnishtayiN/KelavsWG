/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 *
 * Uses the WireGuard Android userspace engine (wireguard-go) through GoBackend.
 */
package com.kelavs.vpn.vpn

import android.content.Context
import android.util.Log
import com.wireguard.android.backend.Backend
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.config.Config
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Real WireGuard tunnel manager.
 *
 * The heavy lifting (TUN device creation, wireguard-go handshakes, socket protection) is done by
 * WireGuard's own [GoBackend]; this class only translates Kelavs [ServerProfile]s into
 * WireGuard [Config]s and keeps observable state for the Compose UI.
 */
class WireGuardTunnelManager(context: Context) : TunnelController {

    private val appContext: Context = context.applicationContext
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val busy = AtomicBoolean(false)

    private val backend: Backend by lazy { GoBackend(appContext) }

    private val tunnel = object : Tunnel {
        override fun getName(): String = TUNNEL_NAME

        override fun onStateChange(newState: Tunnel.State) {
            Log.i(TAG, "Backend reports state=$newState")
            if (newState == Tunnel.State.DOWN && !busy.get()) {
                _state.value = TunnelState.DOWN
                _stats.value = TrafficStats()
            }
        }
    }

    private val _state = MutableStateFlow(TunnelState.DOWN)
    override val state: StateFlow<TunnelState> = _state.asStateFlow()

    private val _stats = MutableStateFlow(TrafficStats())
    override val stats: StateFlow<TrafficStats> = _stats.asStateFlow()

    private val _activeProfile = MutableStateFlow<ServerProfile?>(null)
    override val activeProfile: StateFlow<ServerProfile?> = _activeProfile.asStateFlow()

    /**
     * Builds the WireGuard configuration text from a server profile, applying Kelavs's
     * Iran-specific overrides (MTU clamping, keepalive, DNS fallback, optional IPv6 removal).
     */
    fun buildConfigText(profile: ServerProfile): String = buildString {
        appendLine("[Interface]")
        appendLine("PrivateKey = ${profile.privateKey}")
        appendLine("Address = ${profile.address}")
        val dns = profile.dns?.takeIf { it.isNotBlank() }
            ?: ServerProfile.defaultDns(profile.ipv6Enabled)
        appendLine("DNS = ${if (profile.ipv6Enabled) dns else stripIpv6(dns)}")
        appendLine("MTU = ${ServerProfile.safeMtu(profile.mtu)}")
        appendLine()
        appendLine("[Peer]")
        appendLine("PublicKey = ${profile.publicKey}")
        profile.presharedKey?.takeIf { it.isNotBlank() }?.let { appendLine("PresharedKey = $it") }
        appendLine("AllowedIPs = ${allowedIps(profile)}")
        appendLine("Endpoint = ${profile.endpoint}")
        appendLine("PersistentKeepalive = ${profile.persistentKeepalive ?: ServerProfile.DEFAULT_KEEPALIVE}")
    }

    private fun allowedIps(profile: ServerProfile): String =
        if (profile.ipv6Enabled) profile.allowedIps else stripIpv6(profile.allowedIps)

    private fun stripIpv6(value: String): String =
        value.split(',')
            .map { it.trim() }
            .filterNot { it.isEmpty() || it.contains(':') }
            .joinToString(", ")
            .ifBlank { "0.0.0.0/0" }

    override suspend fun connect(profile: ServerProfile): Boolean {
        if (!busy.compareAndSet(false, true)) return false
        _state.value = TunnelState.CONNECTING
        _activeProfile.value = profile
        return withContext(Dispatchers.IO) {
            try {
                val configText = buildConfigText(profile)
                val config = Config.parse(ByteArrayInputStream(configText.toByteArray()))
                backend.setState(tunnel, Tunnel.State.UP, config)
                _state.value = TunnelState.UP
                _stats.value = TrafficStats(connectedSince = System.currentTimeMillis())
                Log.i(TAG, "Tunnel up via ${profile.endpoint}")
                true
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to bring tunnel up", t)
                _state.value = TunnelState.ERROR
                _stats.value = TrafficStats()
                runCatching { backend.setState(tunnel, Tunnel.State.DOWN, null) }
                false
            } finally {
                busy.set(false)
            }
        }
    }

    override suspend fun disconnect() {
        if (!busy.compareAndSet(false, true)) return
        withContext(Dispatchers.IO) {
            try {
                backend.setState(tunnel, Tunnel.State.DOWN, null)
            } catch (t: Throwable) {
                Log.w(TAG, "Error while tearing tunnel down", t)
            } finally {
                _state.value = TunnelState.DOWN
                _stats.value = TrafficStats()
                busy.set(false)
            }
        }
    }

    override suspend fun toggle(profile: ServerProfile?): Boolean {
        return when (state.value) {
            TunnelState.UP, TunnelState.CONNECTING -> {
                disconnect()
                false
            }
            else -> {
                if (profile == null) {
                    _state.value = TunnelState.ERROR
                    false
                } else {
                    connect(profile)
                }
            }
        }
    }

    override fun refreshStats() {
        if (_state.value != TunnelState.UP) return
        scope.launch {
            try {
                val statistics = backend.getStatistics(tunnel)
                _stats.value = _stats.value.copy(
                    rxBytes = statistics.totalRx(),
                    txBytes = statistics.totalTx()
                )
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to read statistics", t)
            }
        }
    }

    override fun backendVersion(): String = try {
        backend.getVersion().orEmpty()
    } catch (t: Throwable) {
        "unknown"
    }

    companion object {
        const val TUNNEL_NAME = "kelavs0"
        private const val TAG = "KelavsVPN/Tunnel"
    }
}
