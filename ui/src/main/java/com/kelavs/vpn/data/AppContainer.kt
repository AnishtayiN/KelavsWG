/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.data

import android.content.Context
import com.kelavs.vpn.network.SafeHttpClient
import com.kelavs.vpn.storage.SubscriptionDatabase
import com.kelavs.vpn.subscription.SubscriptionParser
import com.kelavs.vpn.update.UpdateManager
import com.kelavs.vpn.vpn.TunnelController
import com.kelavs.vpn.vpn.WireGuardTunnelManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Manual dependency container.
 *
 * Everything the app needs is created once per process and shared through
 * [com.kelavs.vpn.KelavsApplication.container].
 */
class AppContainer(context: Context) {

    private val appContext: Context = context.applicationContext

    val applicationScope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    val settings: SecureSettings = SecureSettings(appContext)

    val database: SubscriptionDatabase = SubscriptionDatabase.getInstance(appContext)

    val subscriptionDao get() = database.subscriptionDao()
    val serverDao get() = database.serverConfigDao()
    val metadataDao get() = database.subscriptionMetadataDao()

    val subscriptionParser: SubscriptionParser = SubscriptionParser(SafeHttpClient.create())

    val tunnelController: TunnelController = WireGuardTunnelManager(appContext)

    val updateManager: UpdateManager = UpdateManager(
        context = appContext,
        metadataDao = metadataDao,
        scope = applicationScope
    )

    private val json = kotlinx.serialization.json.Json { ignoreUnknownKeys = true }

    suspend fun connectLastServer(): Boolean {
        val publicKey = settings.lastSelectedPublicKey ?: return false
        val subscription = subscriptionDao.getSubscriptionSync(
            com.kelavs.vpn.storage.entities.SubscriptionEntity.DEFAULT_ID
        ) ?: return false

        val configs = runCatching {
            json.decodeFromString<List<com.kelavs.vpn.subscription.SubscriptionConfig>>(
                subscription.configsJson
            )
        }.getOrNull() ?: return false

        val config = configs.firstOrNull { it.peerPublicKey == publicKey } ?: return false
        val profile = com.kelavs.vpn.vpn.ServerProfile(
            publicKey = config.peerPublicKey,
            name = config.name,
            endpoint = config.peerEndpoint,
            address = config.address,
            privateKey = config.privateKey,
            dns = config.dns,
            mtu = config.mtu,
            persistentKeepalive = config.persistentKeepalive,
            allowedIps = config.peerAllowedIPs,
            presharedKey = config.peerPresharedKey,
            ipv6Enabled = settings.state.value.ipv6Enabled
        )
        return tunnelController.connect(profile)
    }

    suspend fun disconnectTunnel() = tunnelController.disconnect()

    val isTunnelUp: Boolean
        get() = tunnelController.state.value == com.kelavs.vpn.vpn.TunnelState.UP
}
