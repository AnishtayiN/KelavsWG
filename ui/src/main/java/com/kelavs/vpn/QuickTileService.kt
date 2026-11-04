/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn

import android.content.ComponentName
import android.content.Intent
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import android.util.Log
import com.kelavs.vpn.data.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Quick Settings tile.
 *
 * Toggling the tile really brings the WireGuard tunnel up or down through the shared
 * [AppContainer] - no activity is launched, so it also works from the lock screen.
 */
class QuickTileService : TileService() {

    private var scope: CoroutineScope? = null
    private var toggleJob: Job? = null

    private val container: AppContainer?
        get() = (application as? KelavsApplication)?.container

    override fun onStartListening() {
        super.onStartListening()
        scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        updateTile()
    }

    override fun onStopListening() {
        super.onStopListening()
        scope?.cancel()
        scope = null
    }

    override fun onTileAdded() {
        super.onTileAdded()
        updateTile()
    }

    override fun onClick() {
        super.onClick()
        val app = container ?: return
        if (isLocked) {
            unlockAndRun { toggle(app) }
        } else {
            toggle(app)
        }
    }

    private fun toggle(app: AppContainer) {
        toggleJob?.cancel()
        val jobScope = scope ?: CoroutineScope(Dispatchers.IO + SupervisorJob()).also { scope = it }
        toggleJob = jobScope.launch {
            qsTile?.apply {
                state = Tile.STATE_UNAVAILABLE
                updateTile()
            }
            try {
                if (app.isTunnelUp) {
                    app.disconnectTunnel()
                } else {
                    val started = app.connectLastServer()
                    if (!started) {
                        Log.w(TAG, "No server available for quick toggle")
                        openApp()
                    }
                }
            } catch (t: Throwable) {
                Log.e(TAG, "Quick tile toggle failed", t)
            } finally {
                updateTile()
            }
        }
    }

    private fun openApp() {
        val intent = Intent(this, MainActivity::class.java)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivityAndCollapse(intent)
    }

    private fun updateTile() {
        val tile = qsTile ?: return
        val up = container?.isTunnelUp == true
        tile.state = if (up) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
        tile.label = "Kelavs VPN"
        tile.updateTile()
    }

    override fun onDestroy() {
        scope?.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "KelavsVPN/QuickTile"

        fun requestTileUpdate(context: android.content.Context) {
            runCatching {
                requestListeningState(
                    context,
                    ComponentName(context, QuickTileService::class.java)
                )
            }
        }
    }
}
