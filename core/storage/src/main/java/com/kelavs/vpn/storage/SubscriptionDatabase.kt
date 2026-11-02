package com.kelavs.vpn.storage

import android.content.Context
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.kelavs.vpn.subscription.SubscriptionConfig

/**
 * Room database for storing subscription data, server configs, and connection history.
 */
@Database(
    entities = [
        SubscriptionEntity::class,
        ServerConfigEntity::class,
        ConnectionHistoryEntity::class,
        SubscriptionMetadataEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class SubscriptionDatabase : RoomDatabase() {

    abstract fun subscriptionDao(): SubscriptionDao
    abstract fun serverConfigDao(): ServerConfigDao
    abstract fun connectionHistoryDao(): ConnectionHistoryDao
    abstract fun subscriptionMetadataDao(): SubscriptionMetadataDao

    companion object {
        @Volatile private var INSTANCE: SubscriptionDatabase? = null

        fun getInstance(context: Context): SubscriptionDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    SubscriptionDatabase::class.java,
                    "kelavs_vpn.db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}