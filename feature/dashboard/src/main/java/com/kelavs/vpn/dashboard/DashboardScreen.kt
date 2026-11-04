/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kelavs.vpn.vpn.TrafficStats
import com.kelavs.vpn.vpn.TunnelState

/**
 * Main screen: connection toggle, current server and live traffic counters.
 */
@Composable
fun DashboardScreen(
    state: TunnelState,
    stats: TrafficStats,
    serverName: String?,
    serverEndpoint: String?,
    lastError: String?,
    onToggleConnection: () -> Unit,
    onOpenServers: () -> Unit,
    onOpenSubscription: () -> Unit,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "کلاوس VPN",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Row {
                IconButton(onClick = onOpenSubscription) {
                    Icon(Icons.Filled.CloudDownload, contentDescription = "اشتراک")
                }
                IconButton(onClick = onOpenSettings) {
                    Icon(Icons.Filled.Settings, contentDescription = "تنظیمات")
                }
            }
        }

        Spacer(Modifier.height(8.dp))

        ConnectionStatusCard(state = state, onToggleConnection = onToggleConnection)

        Spacer(Modifier.height(16.dp))

        ServerCard(
            serverName = serverName,
            serverEndpoint = serverEndpoint,
            state = state,
            onOpenServers = onOpenServers
        )

        Spacer(Modifier.height(16.dp))

        StatisticsSection(stats = stats)

        if (lastError != null) {
            Spacer(Modifier.height(16.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
            ) {
                Text(
                    text = lastError,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun ConnectionStatusCard(state: TunnelState, onToggleConnection: () -> Unit) {
    val (label, color) = when (state) {
        TunnelState.UP -> "متصل" to Color(0xFF2E7D32)
        TunnelState.CONNECTING -> "در حال اتصال…" to Color(0xFFF9A825)
        TunnelState.ERROR -> "خطا در اتصال" to MaterialTheme.colorScheme.error
        TunnelState.DOWN -> "قطع" to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Button(
                    onClick = onToggleConnection,
                    modifier = Modifier.size(120.dp),
                    shape = CircleShape,
                    enabled = !state.isBusy,
                    colors = ButtonDefaults.buttonColors(containerColor = color)
                ) {
                    Icon(
                        imageVector = Icons.Filled.PowerSettingsNew,
                        contentDescription = label,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(text = label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (state == TunnelState.UP) "برای قطع اتصال لمس کنید" else "برای اتصال لمس کنید",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ServerCard(
    serverName: String?,
    serverEndpoint: String?,
    state: TunnelState,
    onOpenServers: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Filled.Dns, contentDescription = null)
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = serverName ?: "سروری انتخاب نشده",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = serverEndpoint ?: "برای افزودن سرور روی دکمه زیر بزنید",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenServers, enabled = state != TunnelState.CONNECTING) {
                Icon(Icons.Filled.SwapVert, contentDescription = "تغییر سرور")
            }
        }
    }
}

@Composable
private fun StatisticsSection(stats: TrafficStats) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            title = "دریافت",
            value = formatBytes(stats.rxBytes),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "ارسال",
            value = formatBytes(stats.txBytes),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            title = "مدت اتصال",
            value = formatDuration(stats.durationMillis),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(text = value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun formatBytes(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = listOf("B", "KB", "MB", "GB", "TB")
    var value = bytes.toDouble()
    var index = 0
    while (value >= 1024 && index < units.lastIndex) {
        value /= 1024
        index++
    }
    return String.format("%.1f %s", value, units[index])
}

fun formatDuration(millis: Long): String {
    if (millis <= 0) return "۰۰:۰۰"
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format("%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

@Suppress("unused")
private val refreshIconHint = Icons.Filled.Refresh

@Suppress("unused")
private val shadowText = 14.sp
