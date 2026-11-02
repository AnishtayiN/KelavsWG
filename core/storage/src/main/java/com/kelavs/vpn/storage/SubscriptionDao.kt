package com.kelavs.vpn.storage

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: SubscriptionEntity)

    @Update
    suspend fun update(subscription: SubscriptionEntity)

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    fun getSubscription(id: String): Flow<SubscriptionEntity?>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionSync(id: String): SubscriptionEntity?

    @Query("DELETE FROM subscriptions WHERE id = :id")
    suspend fun deleteSubscription(id: String)
}