package com.kelavs.vpn

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.material3.Material3
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kelavs.vpn.dashboard.DashboardScreen
import com.kelavs.vpn.dashboard.DashboardViewModel
import com.kelavs.vpn.onboarding.OnboardingScreen
import com.kelavs.vpn.servers.ServersScreen
import com.kelavs.vpn.servers.ServersViewModel
import com.kelavs.vpn.settings.SettingsScreen
import com.kelavs.vpn.settings.SettingsViewModel
import com.kelavs.vpn.subscription_ui.SubscriptionScreen
import com.kelavs.vpn.subscription_ui.SubscriptionViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class MainActivity : ComponentActivity() {

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    private val dashboardViewModel: DashboardViewModel by viewModels()
    private val serversViewModel: ServersViewModel by viewModels()
    private val subscriptionViewModel: SubscriptionViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    var showOnboarding by mutableStateOf(false)
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check if subscription exists
        checkSubscriptionStatus()
        
        setContent {
            Material3 {
                Surface {
                    NavHost(rememberNavController(), "dashboard") {
                        composable("dashboard") {
                            DashboardScreen(
                                viewModel = dashboardViewModel,
                                onNavigateToServers = { navController.navigate("servers") },
                                onNavigateToSubscription = { navController.navigate("subscription") },
                                onNavigateToSettings = { navController.navigate("settings") }
                            )
                        }
                        composable("servers") {
                            ServersScreen(
                                viewModel = serversViewModel,
                                onServerSelected = { server ->
                                    dashboardViewModel.selectServer(server)
                                    navController.navigateUp()
                                }
                            )
                        }
                        composable("subscription") {
                            SubscriptionScreen(viewModel = subscriptionViewModel)
                        }
                        composable("settings") {
                            SettingsScreen(
                                viewModel = settingsViewModel,
                                onBackPressed = { navController.navigateUp() }
                            )
                        }
                        composable("onboarding") {
                            OnboardingScreen(
                                onSubscriptionLoaded = { result ->
                                    subscriptionViewModel.onSubscriptionParsed(result)
                                    showOnboarding = false
                                    navController.navigate("dashboard") { popUpTo("dashboard") { inclusive = true } }
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    private fun checkSubscriptionStatus() {
        scope.launch {
            // Check if subscription exists in database
            val db = com.kelavs.vpn.storage.SubscriptionDatabase.getInstance(this)
            val subscription = db.subscriptionDao().getSubscriptionSync("main_subscription")
            showOnboarding = subscription == null
        }
    }
}