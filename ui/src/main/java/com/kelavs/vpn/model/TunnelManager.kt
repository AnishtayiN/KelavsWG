package com.kelavs.vpn.model

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Manages WireGuard tunnel states and handles external control intents.
 */
class TunnelManager {

    companion object {
        private const val TAG = "KelavsVPN/TunnelManager"

        const val ACTION_REFRESH_TUNNEL_STATES = "com.kelavs.vpn.action.REFRESH_TUNNEL_STATES"
        const val ACTION_SET_TUNNEL_UP = "com.kelavs.vpn.action.SET_TUNNEL_UP"
        const val ACTION_SET_TUNNEL_DOWN = "com.kelavs.vpn.action.SET_TUNNEL_DOWN"

        const val EXTRA_TUNNEL_NAME = "tunnel_name"
        const val EXTRA_TUNNEL_PUBLIC_KEY = "tunnel_public_key"
    }

    class IntentReceiver : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_REFRESH_TUNNEL_STATES -> {
                    Log.d(TAG, "Refresh tunnel states requested")
                    // Refresh and broadcast tunnel states
                }
                ACTION_SET_TUNNEL_UP -> {
                    val name = intent.getStringExtra(EXTRA_TUNNEL_NAME)
                    val publicKey = intent.getStringExtra(EXTRA_TUNNEL_PUBLIC_KEY)
                    Log.d(TAG, "Set tunnel up: name=$name, publicKey=$publicKey")
                    // Start tunnel
                }
                ACTION_SET_TUNNEL_DOWN -> {
                    val name = intent.getStringExtra(EXTRA_TUNNEL_NAME)
                    val publicKey = intent.getStringExtra(EXTRA_TUNNEL_PUBLIC_KEY)
                    Log.d(TAG, "Set tunnel down: name=$name, publicKey=$publicKey")
                    // Stop tunnel
                }
            }
        }
    }
}