package com.kelavs.vpn.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Material3
import androidx.compose.material3.Surface
import com.kelavs.vpn.settings.SettingsScreen
import com.kelavs.vpn.settings.SettingsViewModel
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.viewModels

class SettingsActivity : ComponentActivity() {

    private val viewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Material3 {
                Surface {
                    SettingsScreen(
                        viewModel = viewModel,
                        onBackPressed = { finish() }
                    )
                }
            }
        }
    }
}