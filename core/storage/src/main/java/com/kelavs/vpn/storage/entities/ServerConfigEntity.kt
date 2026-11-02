package com.kelavs.vpn.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "server_configs")
@Serializable
data class ServerConfigEntity(
    @PrimaryKey val publicKey: String, // Unique identifier for the server
    val name: String,
    val endpoint: String,
    val address: String,
    val dns: String?,
    val mtu: Int?,
    val persistentKeepalive: Int?,
    val allowedIPs: String,
    val presharedKey: String? = null,
    val sourceSubscriptionUrl: String,
    val addedAt: Long = System.currentTimeMillis(),
    val lastConnected: Long = 0,
    val connectionCount: Int = 0,
    val successCount: Int = 0,
    val failureCount: Int = 0,
    val avgLatencyMs: Long = 0,
    val lastHandshakeAt: Long = 0,
    val isEnabled: Boolean = true,
    val isAutoSelected: Boolean = false
)