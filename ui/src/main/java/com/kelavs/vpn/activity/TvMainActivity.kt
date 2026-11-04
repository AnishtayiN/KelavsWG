/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kelavs.vpn.MainViewModel

/**
 * Leanback (Android TV) entry point. Android TV devices have no touch screen, so the screen is
 * reduced to a large, D-pad friendly connection toggle.
 */
class TvMainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    TvMainScreen(viewModel())
                }
            }
        }
    }
}

@Composable
fun TvMainScreen(viewModel: MainViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Kelavs VPN",
            style = MaterialTheme.typography.displaySmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = when (state.tunnelState) {
                com.kelavs.vpn.vpn.TunnelState.UP -> "متصل"
                com.kelavs.vpn.vpn.TunnelState.CONNECTING -> "در حال اتصال…"
                com.kelavs.vpn.vpn.TunnelState.ERROR -> "خطا در اتصال"
                com.kelavs.vpn.vpn.TunnelState.DOWN -> "قطع"
            },
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 24.dp)
        )
        Button(onClick = viewModel::toggleConnection) {
            Icon(Icons.Filled.SwapVert, contentDescription = null)
            Text(
                text = "تغییر وضعیت اتصال",
                modifier = Modifier.padding(start = 8.dp)
            )
        }
        state.selectedServer?.let { server ->
            Text(
                text = server.name,
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 24.dp)
            )
        }
    }
}
