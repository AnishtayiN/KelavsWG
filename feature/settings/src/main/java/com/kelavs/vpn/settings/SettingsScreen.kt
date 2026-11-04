/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Settings snapshot rendered by [SettingsScreen]. */
data class SettingsState(
    val darkTheme: Boolean,
    val usePersian: Boolean,
    val ipv6Enabled: Boolean,
    val autoReconnect: Boolean,
    val autoUpdateCheck: Boolean,
    val versionName: String,
    val versionCode: Long,
    val updateStatus: String? = null
)

/**
 * Application settings: appearance, network behaviour, updates and support links.
 */
@Composable
fun SettingsScreen(
    state: SettingsState,
    onToggleDarkTheme: (Boolean) -> Unit,
    onToggleLanguage: (Boolean) -> Unit,
    onToggleIpv6: (Boolean) -> Unit,
    onToggleAutoReconnect: (Boolean) -> Unit,
    onToggleAutoUpdate: (Boolean) -> Unit,
    onCheckForUpdates: () -> Unit,
    onOpenTelegramChannel: () -> Unit,
    onOpenTelegramBot: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "بازگشت")
            }
            Text(
                text = "تنظیمات",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        SectionHeader("ظاهر برنامه", Icons.Filled.Palette)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                SwitchRow(
                    title = "حالت تاریک",
                    subtitle = "استفاده از رنگ‌های تیره",
                    checked = state.darkTheme,
                    onCheckedChange = onToggleDarkTheme
                )
                HorizontalDivider()
                SwitchRow(
                    title = "زبان فارسی",
                    subtitle = "رابط کاربری راست‌به‌چپ",
                    checked = state.usePersian,
                    onCheckedChange = onToggleLanguage
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionHeader("شبکه و اتصال", Icons.Filled.Language)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                SwitchRow(
                    title = "پشتیبانی IPv6",
                    subtitle = "در صورت مشکل اتصال، خاموش کنید",
                    checked = state.ipv6Enabled,
                    onCheckedChange = onToggleIpv6
                )
                HorizontalDivider()
                SwitchRow(
                    title = "اتصال مجدد خودکار",
                    subtitle = "پس از تغییر شبکه یا قطعی اینترنت",
                    checked = state.autoReconnect,
                    onCheckedChange = onToggleAutoReconnect
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionHeader("بروزرسانی", Icons.Filled.SystemUpdate)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                SwitchRow(
                    title = "بررسی خودکار بروزرسانی",
                    subtitle = "در هر بار اجرای برنامه",
                    checked = state.autoUpdateCheck,
                    onCheckedChange = onToggleAutoUpdate
                )
                HorizontalDivider()
                ActionRow(
                    title = "بررسی بروزرسانی",
                    subtitle = state.updateStatus ?: "نسخه ${state.versionName}",
                    icon = Icons.Filled.Verified,
                    onClick = onCheckForUpdates
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        SectionHeader("پشتیبانی", Icons.Filled.Send)
        Card(modifier = Modifier.fillMaxWidth()) {
            Column {
                ActionRow(
                    title = "کانال تلگرام",
                    subtitle = "@Matinkelavs",
                    icon = Icons.Filled.Send,
                    onClick = onOpenTelegramChannel
                )
                HorizontalDivider()
                ActionRow(
                    title = "ربات پشتیبانی",
                    subtitle = "@Kelavs_bot",
                    icon = Icons.Filled.Send,
                    onClick = onOpenTelegramBot
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Filled.Info,
                contentDescription = null,
                modifier = Modifier.width(16.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Kelavs VPN ${state.versionName} (${state.versionCode})",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun SectionHeader(title: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun SwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun ActionRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Filled.ChevronLeft, contentDescription = null)
        }
    }
}
