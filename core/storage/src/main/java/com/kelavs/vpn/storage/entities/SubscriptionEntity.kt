package com.kelavs.vpn.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.kelavs.vpn.subscription.SubscriptionConfig
import kotlinx.serialization.Serializable

@Entity(tableName = "subscriptions")
@Serializable
data class SubscriptionEntity(
    @PrimaryKey val id: String = "main_subscription", // Single subscription for now
    val sourceUrl: String,
    val lastUpdated: Long,
    val lastUpdateAttempt: Long,
    val configCount: Int,
    val isValid: Boolean,
    val errorMessage: String? = null,
    val configsJson: String // Serialized list of SubscriptionConfig
) {
    companion object {
        fun create(
            sourceUrl: String,
            configs: List<SubscriptionConfig>,
            lastUpdated: Long = System.currentTimeMillis(),
            lastUpdateAttempt: Long = System.currentTimeMillis(),
            isValid: Boolean = true,
            errorMessage: String? = null
        ): SubscriptionEntity {
            return SubscriptionEntity(
                id = "main_subscription",
                sourceUrl = sourceUrl,
                lastUpdated = lastUpdated,
                lastUpdateAttempt = lastUpdateAttempt,
                configCount = configs.size,
                isValid = isValid,
                errorMessage = errorMessage,
                configsJson = kotlinx.serialization.json.Json.encodeToString(configs)
            )
        }
    }
}