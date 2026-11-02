package com.kelavs.vpn

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import com.kelavs.vpn.storage.SubscriptionDatabase
import com.kelavs.vpn.update.UpdateManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient

/**
 * Main Application class for Kelavs VPN.
 * Initializes database, network clients, and global coroutine scope.
 */
class KelavsApplication : Application() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onCreate() {
        super.onCreate()
        
        // Initialize database
        SubscriptionDatabase.getInstance(this)
        
        // Initialize ProcessLifecycleOwner for WorkManager
        ProcessLifecycleOwner.init(this)
    }

    /**
     * Global coroutine scope for application-level operations.
     */
    val applicationScope: CoroutineScope = scope

    /**
     * Creates a secure HTTP client for subscription fetching.
     */
    fun createSubscriptionHttpClient(): OkHttpClient = com.kelavs.vpn.network.SafeHttpClient.create()

    /**
     * Creates an HTTP client for update checking.
     */
    fun createUpdateHttpClient(): OkHttpClient = com.kelavs.vpn.network.SafeHttpClient.createUpdateClient()

    /**
     * Gets the UpdateManager instance.
     */
    fun getUpdateManager(): UpdateManager = UpdateManager.getInstance(this)
}