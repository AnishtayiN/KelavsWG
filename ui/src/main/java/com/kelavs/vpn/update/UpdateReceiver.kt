package com.kelavs.vpn.update

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receiver for MY_PACKAGE_REPLACED - triggered when app is updated.
 * Can be used to restart services or clear caches.
 */
class UpdateReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "KelavsVPN/UpdateReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.i(TAG, "App updated, performing post-update tasks")
            
            // Clear any cached data that might be incompatible
            // Restart VPN service if it was running
            // Schedule subscription refresh
            
            // For now, just log
            Log.i(TAG, "Post-update tasks completed")
        }
    }
}