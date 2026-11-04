/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Re-establishes the tunnel after a reboot or gracefully closes it before shutdown when the user
 * asked for "always connected" behaviour.
 */
class BootShutdownReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? KelavsApplication ?: return
        val container = app.container

        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED -> {
                if (!container.settings.state.value.autoReconnect) return
                Log.i(TAG, "Boot completed, restoring tunnel")
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        if (container.settings.lastSelectedPublicKey != null) {
                            val started = container.connectLastServer()
                            Log.i(TAG, "Auto reconnect after boot: $started")
                        }
                    } catch (t: Throwable) {
                        Log.e(TAG, "Auto reconnect failed", t)
                    } finally {
                        pending.finish()
                    }
                }
            }

            Intent.ACTION_SHUTDOWN, Intent.ACTION_MY_PACKAGE_REPLACED -> {
                Log.i(TAG, "Shutting down, closing tunnel")
                val pending = goAsync()
                CoroutineScope(Dispatchers.IO + SupervisorJob()).launch {
                    try {
                        container.disconnectTunnel()
                    } finally {
                        pending.finish()
                    }
                }
            }
        }
    }

    private companion object {
        const val TAG = "KelavsVPN/Boot"
    }
}
