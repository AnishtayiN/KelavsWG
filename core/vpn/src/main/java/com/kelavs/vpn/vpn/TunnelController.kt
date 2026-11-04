/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.vpn

import kotlinx.coroutines.flow.StateFlow

/**
 * Connection state exposed to the UI layer.
 */
enum class TunnelState {
    DOWN,
    CONNECTING,
    UP,
    ERROR;

    val isConnected: Boolean get() = this == UP
    val isBusy: Boolean get() = this == CONNECTING
}

/**
 * Traffic counters reported by the WireGuard backend.
 */
data class TrafficStats(
    val rxBytes: Long = 0L,
    val txBytes: Long = 0L,
    val latestHandshakeEpochMillis: Long = 0L,
    val connectedSince: Long = 0L
) {
    val durationMillis: Long
        get() = if (connectedSince > 0L) {
            (System.currentTimeMillis() - connectedSince).coerceAtLeast(0L)
        } else {
            0L
        }

    val hasHandshake: Boolean
        get() = latestHandshakeEpochMillis > 0L
}

/**
 * Server profile as stored locally, plus the transformation into a WireGuard configuration.
 *
 * Kelavs applies Iran-specific defaults on top of whatever the subscription provided:
 *  - MTU is clamped to a tunnel-safe range (1280 by default) to survive DPI/MTU black holes.
 *  - DNS falls back to non-filtered resolvers when the subscription does not specify any.
 *  - PersistentKeepalive is enabled so the tunnel survives NAT timeouts on Iranian ISPs.
 */
data class ServerProfile(
    val publicKey: String,
    val name: String,
    val endpoint: String,
    val address: String,
    val privateKey: String,
    val dns: String? = null,
    val mtu: Int? = null,
    val persistentKeepalive: Int? = null,
    val allowedIps: String = "0.0.0.0/0, ::/0",
    val presharedKey: String? = null,
    val ipv6Enabled: Boolean = true
) {
    companion object {
        const val DEFAULT_MTU = 1420
        const val IRAN_SAFE_MTU = 1280
        const val DEFAULT_KEEPALIVE = 25
        const val DEFAULT_DNS_V4 = "1.1.1.1, 8.8.8.8"
        const val DEFAULT_DNS_V6 = "2606:4700:4700::1111, 2001:4860:4860::8888"

        /**
         * Address family aware defaults. IPv6 is only added when the user has not opted out,
         * because a broken IPv6 path is the most common cause of "connected but no internet"
         * on Iranian mobile carriers.
         */
        fun defaultDns(ipv6Enabled: Boolean): String =
            if (ipv6Enabled) "$DEFAULT_DNS_V4, $DEFAULT_DNS_V6" else DEFAULT_DNS_V4

        fun safeMtu(requested: Int?): Int {
            val value = requested ?: DEFAULT_MTU
            return value.coerceIn(IRAN_SAFE_MTU, 1420)
        }
    }
}

/**
 * Operations the UI needs from the tunnel layer.
 * Implemented by [WireGuardTunnelManager] on top of WireGuard's GoBackend.
 */
interface TunnelController {

    /** Current state of the tunnel. */
    val state: StateFlow<TunnelState>

    /** Latest traffic counters. */
    val stats: StateFlow<TrafficStats>

    /** Profile currently used (or last used) by the tunnel. */
    val activeProfile: StateFlow<ServerProfile?>

    /**
     * Bring the tunnel up using the given profile.
     * Returns true when the interface was established and handed to wireguard-go.
     */
    suspend fun connect(profile: ServerProfile): Boolean

    /** Tear the tunnel down. */
    suspend fun disconnect()

    /** Toggle the tunnel; connects when down, disconnects when up. */
    suspend fun toggle(profile: ServerProfile?): Boolean

    /** Refresh traffic counters from the backend. */
    fun refreshStats()

    /** Human readable backend version, e.g. "wireguard-go v0.0.20230223". */
    fun backendVersion(): String
}
