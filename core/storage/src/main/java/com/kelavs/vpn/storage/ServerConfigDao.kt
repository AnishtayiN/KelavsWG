package com.kelavs.vpn.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ServerConfigDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(configs: List<ServerConfigEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(config: ServerConfigEntity)

    @Update
    suspend fun update(config: ServerConfigEntity)

    @Query("SELECT * FROM server_configs ORDER BY name ASC")
    fun getAllServers(): Flow<List<ServerConfigEntity>>

    @Query("SELECT * FROM server_configs WHERE isEnabled = 1 ORDER BY name ASC")
    fun getEnabledServers(): Flow<List<ServerConfigEntity>>

    @Query("SELECT * FROM server_configs WHERE publicKey = :publicKey")
    fun getServer(publicKey: String): Flow<ServerConfigEntity?>

    @Query("SELECT * FROM server_configs WHERE publicKey = :publicKey")
    suspend fun getServerSync(publicKey: String): ServerConfigEntity?

    @Query("SELECT * FROM server_configs WHERE isAutoSelected = 1")
    fun getAutoSelectedServer(): Flow<ServerConfigEntity?>

    @Query("SELECT * FROM server_configs WHERE isAutoSelected = 1")
    suspend fun getAutoSelectedServerSync(): ServerConfigEntity?

    @Query("UPDATE server_configs SET isAutoSelected = 0")
    suspend fun clearAutoSelection()

    @Query("UPDATE server_configs SET isAutoSelected = 1 WHERE publicKey = :publicKey")
    suspend fun setAutoSelected(publicKey: String)

    @Query("UPDATE server_configs SET lastConnected = :time, connectionCount = connectionCount + 1 WHERE publicKey = :publicKey")
    suspend fun recordConnection(publicKey: String, time: Long)

    @Query("UPDATE server_configs SET successCount = successCount + 1, lastHandshakeAt = :time WHERE publicKey = :publicKey")
    suspend fun recordSuccess(publicKey: String, time: Long)

    @Query("UPDATE server_configs SET failureCount = failureCount + 1 WHERE publicKey = :publicKey")
    suspend fun recordFailure(publicKey: String)

    @Query("UPDATE server_configs SET avgLatencyMs = :latency WHERE publicKey = :publicKey")
    suspend fun updateAvgLatency(publicKey: String, latency: Long)

    @Query("DELETE FROM server_configs WHERE publicKey = :publicKey")
    suspend fun deleteServer(publicKey: String)

    @Query("DELETE FROM server_configs WHERE sourceSubscriptionUrl = :url")
    suspend fun deleteBySubscriptionUrl(url: String)

    @Query("UPDATE server_configs SET isEnabled = :enabled WHERE publicKey = :publicKey")
    suspend fun setEnabled(publicKey: String, enabled: Boolean)
}