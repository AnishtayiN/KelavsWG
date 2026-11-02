package com.kelavs.vpn.activity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.Material3
import androidx.compose.material3.Surface
import com.kelavs.vpn.subscription_ui.SubscriptionScreen
import com.kelavs.vpn.subscription_ui.SubscriptionViewModel
import androidx.lifecycle.viewmodel.viewModels

class SubscriptionActivity : ComponentActivity() {

    private val viewModel: SubscriptionViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Material3 {
                Surface {
                    SubscriptionScreen(viewModel = viewModel)
                }
            }
        }
    }
}