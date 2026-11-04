/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.subscription_ui

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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

/**
 * Subscription management: enter / change the subscription URL, refresh it manually and
 * remove it. The same composable is reused by the first-run onboarding flow.
 */
@Composable
fun SubscriptionScreen(
    currentUrl: String?,
    configCount: Int,
    lastUpdated: Long?,
    isRefreshing: Boolean,
    errorMessage: String?,
    onSave: (String) -> Unit,
    onRefresh: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = "اشتراک"
) {
    var url by remember(currentUrl) { mutableStateOf(currentUrl.orEmpty()) }

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
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = url,
            onValueChange = { url = it.trim() },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("آدرس اشتراک (Subscription URL)") },
            singleLine = false,
            minLines = 2,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            enabled = !isRefreshing
        )

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = { onSave(url) },
            modifier = Modifier.fillMaxWidth(),
            enabled = url.isNotBlank() && !isRefreshing
        ) {
            Icon(Icons.Filled.CloudDownload, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text(if (isRefreshing) "در حال دریافت…" else "ذخیره و دریافت سرورها")
        }

        if (isRefreshing) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                CircularProgressIndicator()
            }
        }

        if (errorMessage != null) {
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer,
                    textAlign = TextAlign.Center
                )
            }
        }

        if (currentUrl != null) {
            Spacer(Modifier.height(24.dp))
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "وضعیت اشتراک",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(Modifier.height(8.dp))
                    InfoRow("تعداد سرورها", configCount.toString())
                    InfoRow(
                        "آخرین بروزرسانی",
                        lastUpdated?.let { formatTimestamp(it) } ?: "—"
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                    enabled = !isRefreshing
                ) { Text("بروزرسانی") }
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    enabled = !isRefreshing
                ) {
                    Icon(Icons.Filled.Delete, contentDescription = null)
                    Spacer(Modifier.width(4.dp))
                    Text("حذف")
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

internal fun formatTimestamp(millis: Long): String {
    val formatter = java.text.SimpleDateFormat("yyyy/MM/dd HH:mm", java.util.Locale.US)
    return formatter.format(java.util.Date(millis))
}
