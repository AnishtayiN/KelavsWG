package com.kelavs.vpn.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "connection_history")
data class ConnectionHistoryEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val serverPublicKey: String,
    val serverName: String,
    val endpoint: String,
    val startTime: Long,
    val endTime: Long?,
    val bytesReceived: Long = 0,
    val bytesSent: Long = 0,
    val success: Boolean,
    val errorMessage: String? = null,
    val handshakeCount: Int = 0,
    val avgLatencyMs: Long = 0
)