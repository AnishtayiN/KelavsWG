package com.kelavs.vpn.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import com.kelavs.vpn.storage.entities.SubscriptionMetadataEntity

@Dao
interface SubscriptionMetadataDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(metadata: SubscriptionMetadataEntity)

    @Update
    suspend fun update(metadata: SubscriptionMetadataEntity)

    @Query("SELECT * FROM subscription_metadata WHERE key = :key")
    suspend fun get(key: String): SubscriptionMetadataEntity?

    @Query("SELECT value FROM subscription_metadata WHERE key = :key")
    suspend fun getValue(key: String): String?

    @Query("DELETE FROM subscription_metadata WHERE key = :key")
    suspend fun delete(key: String)

    companion object {
        const val KEY_SUBSCRIPTION_URL = "subscription_url"
        const val KEY_SUBSCRIPTION_ENCRYPTED = "subscription_encrypted"
        const val KEY_AUTO_UPDATE_ENABLED = "auto_update_enabled"
        const val KEY_LAST_UPDATE_CHECK = "last_update_check"
        const val KEY_SELECTED_SERVER = "selected_server_public_key"
        const val KEY_AUTO_SERVER_SELECTION = "auto_server_selection"
        const val KEY_MTU = "mtu"
        const val KEY_CUSTOM_DNS = "custom_dns"
        const val KEY_THEME = "theme"
        const val KEY_LANGUAGE = "language"
    }
}