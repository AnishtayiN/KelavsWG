package com.kelavs.vpn.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ConnectionHistoryDao {

    @Insert
    suspend fun insert(history: ConnectionHistoryEntity): Long

    @Update
    suspend fun update(history: ConnectionHistoryEntity)

    @Query("SELECT * FROM connection_history WHERE serverPublicKey = :publicKey ORDER BY startTime DESC LIMIT :limit")
    fun getHistoryForServer(publicKey: String, limit: Int): Flow<List<ConnectionHistoryEntity>>

    @Query("SELECT * FROM connection_history ORDER BY startTime DESC LIMIT :limit")
    fun getRecentHistory(limit: Int): Flow<List<ConnectionHistoryEntity>>

    @Query("SELECT * FROM connection_history WHERE endTime IS NULL AND serverPublicKey = :publicKey")
    suspend fun getActiveConnection(publicKey: String): ConnectionHistoryEntity?

    @Query("DELETE FROM connection_history WHERE startTime < :olderThan")
    suspend fun cleanupOldHistory(olderThan: Long)
}