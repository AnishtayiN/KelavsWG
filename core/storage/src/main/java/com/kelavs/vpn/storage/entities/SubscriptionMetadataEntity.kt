package com.kelavs.vpn.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_metadata")
data class SubscriptionMetadataEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)