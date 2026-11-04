/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.kelavs.vpn.data.AppContainer

/**
 * Kelavs VPN application entry point.
 *
 * Keeps a single [AppContainer] alive for the whole process. A manual container is used instead
 * of a DI framework so that the build stays simple and fast (no annotation processing for Hilt).
 */
class KelavsApplication : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_VPN,
            "وضعیت اتصال",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "نمایش وضعیت اتصال Kelavs VPN"
            setShowBadge(false)
        }
        manager.createNotificationChannel(channel)
    }

    companion object {
        const val CHANNEL_VPN = "kelavs_vpn_status"
    }
}
