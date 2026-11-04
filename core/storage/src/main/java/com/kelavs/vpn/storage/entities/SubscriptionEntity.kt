/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.storage.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Stored subscription.
 *
 * The parsed configurations are kept as a JSON payload ([configsJson]) so that the storage
 * module does not need to depend on the subscription parser (which would create a module cycle).
 */
@Entity(tableName = "subscriptions")
data class SubscriptionEntity(
    @PrimaryKey val id: String = DEFAULT_ID,
    val sourceUrl: String,
    val lastUpdated: Long,
    val lastUpdateAttempt: Long,
    val configCount: Int,
    val isValid: Boolean,
    val errorMessage: String? = null,
    val configsJson: String
) {
    companion object {
        const val DEFAULT_ID = "main_subscription"

        fun create(
            sourceUrl: String,
            configsJson: String,
            configCount: Int,
            lastUpdated: Long = System.currentTimeMillis(),
            lastUpdateAttempt: Long = System.currentTimeMillis(),
            isValid: Boolean = true,
            errorMessage: String? = null
        ): SubscriptionEntity = SubscriptionEntity(
            id = DEFAULT_ID,
            sourceUrl = sourceUrl,
            lastUpdated = lastUpdated,
            lastUpdateAttempt = lastUpdateAttempt,
            configCount = configCount,
            isValid = isValid,
            errorMessage = errorMessage,
            configsJson = configsJson
        )
    }
}
