package com.kelavs.vpn

import android.app.Service
import android.content.Intent
import android.net.VpnService
import android.os.IBinder
import com.wireguard.android.backend.GoBackend
import com.wireguard.android.backend.Tunnel
import com.wireguard.android.util.SharedLibraryLoader
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/**
 * Kelavs VPN Service - Extends WireGuard's VpnService implementation.
 * Manages the WireGuard tunnel lifecycle.
 */
class KelavsVpnService : VpnService() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var tunnel: Tunnel? = null

    override fun onCreate() {
        super.onCreate()
        // Load WireGuard native libraries
        SharedLibraryLoader.load(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Handle tunnel start/stop commands
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onDestroy() {
        stopTunnel()
        scope.coroutineContext.cancelChildren()
        super.onDestroy()
    }

    fun startTunnel(config: String): Boolean {
        return try {
            if (tunnel != null) {
                stopTunnel()
            }
            
            // Prepare VPN interface
            val builder = android.net.VpnService.Builder()
            val iface = builder
                .setSession("Kelavs VPN")
                .addAddress("0.0.0.0", 0) // Will be configured by WireGuard
                .addRoute("0.0.0.0", 0)
                .addDnsServer("1.1.1.1")
                .addDnsServer("8.8.8.8")
                .establish()
            
            // Create WireGuard tunnel using Go backend
            tunnel = GoBackend(this).createTunnel(iface.fileDescriptor, config)
            tunnel?.start()
            
            // Show foreground notification
            showNotification()
            
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun stopTunnel() {
        tunnel?.close()
        tunnel = null
        stopForeground(true)
    }

    private fun showNotification() {
        val notification = androidx.core.app.NotificationCompat.Builder(this, "kelavs_vpn")
            .setContentTitle("Kelavs VPN")
            .setContentText("متصل - برای قطع کردن روی این پیام بزنید")
            .setSmallIcon(R.drawable.ic_vpn_notification)
            .setOngoing(true)
            .setCategory(androidx.core.app.NotificationCompat.CATEGORY_SERVICE)
            .setPriority(androidx.core.app.NotificationCompat.PRIORITY_LOW)
            .build()

        startForeground(1, notification)
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "kelavs_vpn"
    }
}