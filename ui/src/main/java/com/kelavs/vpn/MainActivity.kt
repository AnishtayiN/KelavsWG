/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.runtime.collectAsState
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kelavs.vpn.dashboard.DashboardScreen
import com.kelavs.vpn.onboarding.OnboardingScreen
import com.kelavs.vpn.servers.ServersScreen
import com.kelavs.vpn.settings.SettingsScreen
import com.kelavs.vpn.subscription_ui.SubscriptionScreen
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val state by viewModel.uiState.collectAsState()
            val darkTheme = state.settings.darkTheme
            val usePersian = state.settings.usePersian

            val layoutDirection = if (usePersian) LayoutDirection.Rtl else LayoutDirection.Ltr
            val locale = if (usePersian) Locale("fa", "IR") else Locale.ENGLISH

            CompositionLocalProviderWrapper(layoutDirection = layoutDirection, locale = locale) {
                KelavsTheme(darkTheme = darkTheme) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        KelavsNavHost(viewModel = viewModel, state = state)
                    }
                }
            }
        }
    }
}

@Composable
private fun CompositionLocalProviderWrapper(
    layoutDirection: LayoutDirection,
    locale: Locale,
    content: @Composable () -> Unit
) {
    androidx.compose.runtime.CompositionLocalProvider(
        LocalLayoutDirection provides layoutDirection
    ) {
        androidx.compose.runtime.key(locale) { content() }
    }
}

@Composable
private fun KelavsTheme(darkTheme: Boolean, content: @Composable () -> Unit) {
    val colorScheme = if (darkTheme) darkColorScheme(
        primary = Color(0xFF4CAF50),
        secondary = Color(0xFF81C784)
    ) else lightColorScheme(
        primary = Color(0xFF2E7D32),
        secondary = Color(0xFF66BB6A)
    )
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
private fun KelavsNavHost(viewModel: MainViewModel, state: UiState) {
    val navController: NavHostController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.consumeMessage()
        }
    }

    val startDestination = if (state.onboardingRequired) Routes.ONBOARDING else Routes.DASHBOARD

    Scaffold(snackbarHost = { SnackbarHost(snackbarHostState) }) { padding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(padding)
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    isLoading = state.isImporting,
                    errorMessage = state.error,
                    onImport = { url -> viewModel.importSubscription(url) },
                    onSkip = {
                        viewModel.dismissOnboarding()
                        navController.navigate(Routes.DASHBOARD) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.DASHBOARD) {
                DashboardScreen(
                    state = state.tunnelState,
                    stats = state.stats,
                    serverName = state.selectedServer?.name,
                    serverEndpoint = state.selectedServer?.endpoint,
                    lastError = state.error,
                    onToggleConnection = viewModel::toggleConnection,
                    onOpenServers = { navController.navigate(Routes.SERVERS) },
                    onOpenSubscription = { navController.navigate(Routes.SUBSCRIPTION) },
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) }
                )
            }

            composable(Routes.SERVERS) {
                ServersScreen(
                    servers = viewModel.serverRows(),
                    isOptimizing = state.tunnelState.isBusy,
                    onSelect = { row ->
                        viewModel.selectServer(row.publicKey)
                        navController.popBackStack()
                    },
                    onAutoSelect = { viewModel.autoSelectBestServer() },
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SUBSCRIPTION) {
                SubscriptionScreen(
                    currentUrl = state.subscriptionUrl,
                    configCount = state.subscription?.configCount ?: 0,
                    lastUpdated = state.subscription?.lastUpdated,
                    isRefreshing = state.isImporting,
                    errorMessage = state.error,
                    onSave = viewModel::importSubscription,
                    onRefresh = viewModel::refreshSubscription,
                    onDelete = viewModel::deleteSubscription,
                    onBack = { navController.popBackStack() }
                )
            }

            composable(Routes.SETTINGS) {
                SettingsScreen(
                    state = viewModel.settingsState(),
                    onToggleDarkTheme = viewModel::setDarkTheme,
                    onToggleLanguage = viewModel::setUsePersian,
                    onToggleIpv6 = viewModel::setIpv6,
                    onToggleAutoReconnect = viewModel::setAutoReconnect,
                    onToggleAutoUpdate = viewModel::setAutoUpdate,
                    onCheckForUpdates = viewModel::checkForUpdates,
                    onOpenTelegramChannel = { openUrl(context, TELEGRAM_CHANNEL) },
                    onOpenTelegramBot = { openUrl(context, TELEGRAM_BOT) },
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}

private fun openUrl(context: android.content.Context, url: String) {
    runCatching {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}

private object Routes {
    const val ONBOARDING = "onboarding"
    const val DASHBOARD = "dashboard"
    const val SERVERS = "servers"
    const val SUBSCRIPTION = "subscription"
    const val SETTINGS = "settings"
}

private const val TELEGRAM_CHANNEL = "https://t.me/Matinkelavs"
private const val TELEGRAM_BOT = "https://t.me/Kelavs_bot"
