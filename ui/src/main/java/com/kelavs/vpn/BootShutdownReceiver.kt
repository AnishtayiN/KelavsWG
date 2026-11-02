package com.kelavs.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Handles BOOT_COMPLETED and ACTION_SHUTDOWN to manage VPN tunnel lifecycle.
 */
class BootShutdownReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "KelavsVPN/BootShutdownReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                Log.i(TAG, "Boot completed, checking for auto-start VPN")
                // Check if VPN should auto-start
                // Start subscription refresh WorkManager
            }
            Intent.ACTION_SHUTDOWN -> {
                Log.i(TAG, "System shutdown, cleaning up")
                // Gracefully stop VPN tunnel
            }
        }
    }
}