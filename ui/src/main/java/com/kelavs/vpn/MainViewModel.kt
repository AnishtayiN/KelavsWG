/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.kelavs.vpn.data.AppContainer
import com.kelavs.vpn.dashboard.formatBytes
import com.kelavs.vpn.servers.ServerRow
import com.kelavs.vpn.settings.SettingsState
import com.kelavs.vpn.storage.entities.ServerConfigEntity
import com.kelavs.vpn.storage.entities.SubscriptionEntity
import com.kelavs.vpn.subscription.SubscriptionConfig
import com.kelavs.vpn.update.DownloadProgress
import com.kelavs.vpn.update.UpdateState
import com.kelavs.vpn.vpn.ServerProfile
import com.kelavs.vpn.vpn.TrafficStats
import com.kelavs.vpn.vpn.TunnelState
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

/**
 * Single source of truth for the Compose UI.
 *
 * Talks to the real WireGuard backend (through [AppContainer.tunnelController]), the real
 * subscription parser, the Room database and the GitHub based updater.
 */
class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val container: AppContainer =
        (application as KelavsApplication).container

    private val settings = container.settings
    private val parser = container.subscriptionParser
    private val tunnel = container.tunnelController
    private val updateManager = container.updateManager

    private val json = Json { ignoreUnknownKeys = true }

    private val _uiState = MutableStateFlow(UiState())
    val uiState: StateFlow<UiState> = _uiState.asStateFlow()

    private val _servers = MutableStateFlow<List<ServerConfigEntity>>(emptyList())
    val servers: StateFlow<List<ServerConfigEntity>> = _servers.asStateFlow()

    private var statsJob: Job? = null
    private var configCache: Map<String, SubscriptionConfig> = emptyMap()

    init {
        viewModelScope.launch {
            settings.state.collect { snapshot ->
                _uiState.value = _uiState.value.copy(settings = snapshot)
            }
        }
        viewModelScope.launch {
            container.serverDao.getAllServers().collect { list ->
                _servers.value = list
            }
        }
        viewModelScope.launch {
            tunnel.state.collect { state ->
                _uiState.value = _uiState.value.copy(tunnelState = state)
                if (state == TunnelState.UP) startStatsPolling() else stopStatsPolling()
            }
        }
        viewModelScope.launch {
            tunnel.stats.collect { stats ->
                _uiState.value = _uiState.value.copy(stats = stats)
            }
        }
        viewModelScope.launch {
            updateManager.updateState.collect { state ->
                _uiState.value = _uiState.value.copy(updateStatus = state.describe())
            }
        }
        viewModelScope.launch {
            updateManager.downloadProgress.collect { progress ->
                _uiState.value = _uiState.value.copy(downloadProgress = progress)
            }
        }
        viewModelScope.launch { restoreSubscription() }
        viewModelScope.launch { maybeCheckForUpdates() }
    }

    // ---------------------------------------------------------------- subscription

    private suspend fun restoreSubscription() {
        val stored = container.subscriptionDao.getSubscriptionSync(SubscriptionEntity.DEFAULT_ID)
        if (stored != null) {
            configCache = decodeConfigs(stored.configsJson)
            _uiState.value = _uiState.value.copy(
                subscription = stored,
                subscriptionUrl = stored.sourceUrl,
                onboardingRequired = false
            )
            restoreSelection()
        } else {
            _uiState.value = _uiState.value.copy(onboardingRequired = !settings.state.value.onboardingDone)
        }
    }

    private suspend fun restoreSelection() {
        val publicKey = settings.lastSelectedPublicKey ?: return
        val server = container.serverDao.getServerSync(publicKey) ?: return
        _uiState.value = _uiState.value.copy(selectedServer = server)
    }

    fun importSubscription(url: String) {
        val normalized = url.trim()
        if (normalized.isEmpty()) {
            setError("لطفاً لینک اشتراک را وارد کنید")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isImporting = true, error = null)
            parser.parseSubscription(normalized)
                .onSuccess { result -> persistSubscription(normalized, result) }
                .onFailure { error ->
                    Log.e(TAG, "Subscription import failed", error)
                    _uiState.value = _uiState.value.copy(
                        isImporting = false,
                        error = error.toPersianMessage()
                    )
                }
        }
    }

    fun refreshSubscription() {
        val url = _uiState.value.subscriptionUrl ?: return
        importSubscription(url)
    }

    private suspend fun persistSubscription(
        url: String,
        result: com.kelavs.vpn.subscription.SubscriptionParseResult
    ) {
        val configs = result.configs
        if (configs.isEmpty()) {
            _uiState.value = _uiState.value.copy(
                isImporting = false,
                error = "هیچ سروری در این اشتراک پیدا نشد"
            )
            return
        }

        val entity = SubscriptionEntity.create(
            sourceUrl = url,
            configsJson = json.encodeToString(configs),
            configCount = configs.size
        )
        container.subscriptionDao.insert(entity)

        container.serverDao.insertAll(configs.map { it.toEntity(url) })
        configCache = configs.associateBy { it.peerPublicKey }

        settings.subscriptionUrl = url
        settings.setOnboardingDone(true)

        _uiState.value = _uiState.value.copy(
            isImporting = false,
            subscription = entity,
            subscriptionUrl = url,
            onboardingRequired = false,
            error = null,
            message = "${configs.size} سرور با موفقیت اضافه شد"
        )

        if (settings.lastSelectedPublicKey == null) autoSelectBestServer()
    }

    fun deleteSubscription() {
        viewModelScope.launch {
            container.subscriptionDao.deleteSubscription(SubscriptionEntity.DEFAULT_ID)
            _servers.value.forEach { container.serverDao.deleteServer(it.publicKey) }
            configCache = emptyMap()
            settings.subscriptionUrl = null
            settings.lastSelectedPublicKey = null
            _uiState.value = _uiState.value.copy(
                subscription = null,
                subscriptionUrl = null,
                selectedServer = null,
                onboardingRequired = true
            )
        }
    }

    private fun decodeConfigs(payload: String): Map<String, SubscriptionConfig> = try {
        json.decodeFromString<List<SubscriptionConfig>>(payload).associateBy { it.peerPublicKey }
    } catch (t: Throwable) {
        Log.e(TAG, "Unable to decode stored configs", t)
        emptyMap()
    }

    // ---------------------------------------------------------------- servers

    fun serverRows(): List<ServerRow> {
        val selectedKey = _uiState.value.selectedServer?.publicKey
        return _servers.value.map { server ->
            ServerRow(
                publicKey = server.publicKey,
                name = server.name,
                endpoint = server.endpoint,
                isSelected = server.publicKey == selectedKey,
                isHealthy = server.failureCount < 3,
                avgLatencyMs = server.avgLatencyMs
            )
        }
    }

    fun selectServer(publicKey: String) {
        viewModelScope.launch {
            val server = container.serverDao.getServerSync(publicKey) ?: return@launch
            container.serverDao.clearAutoSelection()
            container.serverDao.setAutoSelected(publicKey)
            settings.lastSelectedPublicKey = publicKey
            _uiState.value = _uiState.value.copy(selectedServer = server)

            // Reconnect transparently when the tunnel is already up.
            if (_uiState.value.tunnelState == TunnelState.UP) {
                val profile = buildProfile(server)
                if (profile != null) {
                    tunnel.disconnect()
                    tunnel.connect(profile)
                }
            }
        }
    }

    /**
     * Picks the best server from real measurements: few failures first, then the lowest
     * measured handshake latency, then the most recent successful handshake.
     */
    fun autoSelectBestServer() {
        val candidates = _servers.value.filter { it.isEnabled }
        if (candidates.isEmpty()) {
            setError("سروری برای انتخاب خودکار موجود نیست")
            return
        }
        val best = candidates.sortedWith(
            compareBy<ServerConfigEntity> { it.failureCount }
                .thenBy { if (it.avgLatencyMs <= 0) Long.MAX_VALUE else it.avgLatencyMs }
                .thenByDescending { it.successCount }
        ).first()
        selectServer(best.publicKey)
        _uiState.value = _uiState.value.copy(message = "سرور «${best.name}» انتخاب شد")
    }

    // ---------------------------------------------------------------- tunnel

    fun toggleConnection() {
        viewModelScope.launch {
            if (_uiState.value.tunnelState == TunnelState.UP ||
                _uiState.value.tunnelState == TunnelState.CONNECTING
            ) {
                tunnel.disconnect()
                recordHistory(connected = false)
                return@launch
            }

            var server = _uiState.value.selectedServer
            if (server == null) {
                autoSelectBestServer()
                server = _uiState.value.selectedServer
                    ?: container.serverDao.getAutoSelectedServerSync()
            }
            if (server == null) {
                setError("ابتدا یک سرور انتخاب کنید")
                return@launch
            }

            val profile = buildProfile(server)
            if (profile == null) {
                setError("اطلاعات این سرور ناقص است. اشتراک را بروزرسانی کنید.")
                return@launch
            }

            val startedAt = System.currentTimeMillis()
            val connected = tunnel.connect(profile)
            if (connected) {
                container.serverDao.recordConnection(server.publicKey, startedAt)
                container.serverDao.recordSuccess(server.publicKey, System.currentTimeMillis())
                settings.lastConnectedAt = startedAt
                _uiState.value = _uiState.value.copy(error = null, message = null)
            } else {
                container.serverDao.recordFailure(server.publicKey)
                setError("اتصال به سرور برقرار نشد. سرور دیگری را امتحان کنید.")
            }
        }
    }

    private fun recordHistory(connected: Boolean) {
        if (connected) return
        val stats = _uiState.value.stats
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(stats = TrafficStats())
            Log.i(TAG, "Session closed after ${stats.durationMillis} ms")
        }
    }

    private fun buildProfile(server: ServerConfigEntity): ServerProfile? {
        val config = configCache[server.publicKey] ?: return null
        return ServerProfile(
            publicKey = config.peerPublicKey,
            name = config.name,
            endpoint = config.peerEndpoint,
            address = config.address,
            privateKey = config.privateKey,
            dns = config.dns,
            mtu = config.mtu,
            persistentKeepalive = config.persistentKeepalive,
            allowedIps = config.peerAllowedIPs,
            presharedKey = config.peerPresharedKey,
            ipv6Enabled = settings.state.value.ipv6Enabled
        )
    }

    private fun startStatsPolling() {
        if (statsJob?.isActive == true) return
        statsJob = viewModelScope.launch {
            while (isActive) {
                tunnel.refreshStats()
                delay(STATS_INTERVAL_MS)
            }
        }
    }

    private fun stopStatsPolling() {
        statsJob?.cancel()
        statsJob = null
    }

    // ---------------------------------------------------------------- settings

    fun settingsState(): SettingsState = with(_uiState.value) {
        SettingsState(
            darkTheme = settings.darkTheme,
            usePersian = settings.usePersian,
            ipv6Enabled = settings.ipv6Enabled,
            autoReconnect = settings.autoReconnect,
            autoUpdateCheck = settings.autoUpdateCheck,
            versionName = BuildConfig.VERSION_NAME,
            versionCode = BuildConfig.VERSION_CODE.toLong(),
            updateStatus = updateStatus
        )
    }

    fun setDarkTheme(value: Boolean) = settings.setDarkTheme(value)
    fun setUsePersian(value: Boolean) = settings.setUsePersian(value)
    fun setIpv6(value: Boolean) = settings.setIpv6Enabled(value)
    fun setAutoReconnect(value: Boolean) = settings.setAutoReconnect(value)
    fun setAutoUpdate(value: Boolean) = settings.setAutoUpdateCheck(value)

    // ---------------------------------------------------------------- updates

    fun checkForUpdates() {
        updateManager.checkForUpdates()
    }

    fun downloadAndInstallUpdate() {
        viewModelScope.launch {
            updateManager.downloadApk()
            val state = updateManager.updateState.first { it is UpdateState.Downloaded || it is UpdateState.Error }
            if (state is UpdateState.Error) setError(state.message)
        }
    }

    private suspend fun maybeCheckForUpdates() {
        if (!settings.state.value.autoUpdateCheck) return
        delay(3_000)
        updateManager.checkForUpdates()
    }

    private fun setError(message: String) {
        _uiState.value = _uiState.value.copy(error = message, isImporting = false)
    }

    fun consumeMessage() {
        _uiState.value = _uiState.value.copy(message = null)
    }

    fun dismissOnboarding() {
        settings.setOnboardingDone(true)
        _uiState.value = _uiState.value.copy(onboardingRequired = false)
    }

    override fun onCleared() {
        stopStatsPolling()
        super.onCleared()
    }

    private fun SubscriptionConfig.toEntity(sourceUrl: String) = ServerConfigEntity(
        publicKey = peerPublicKey,
        name = name,
        endpoint = peerEndpoint,
        address = address,
        dns = dns,
        mtu = mtu,
        persistentKeepalive = persistentKeepalive,
        allowedIPs = peerAllowedIPs,
        presharedKey = peerPresharedKey,
        sourceSubscriptionUrl = sourceUrl
    )

    private fun UpdateState.describe(): String = when (this) {
        is UpdateState.Idle -> ""
        is UpdateState.Checking -> "در حال بررسی بروزرسانی…"
        is UpdateState.UpdateAvailable -> "نسخه $version موجود است"
        is UpdateState.Downloaded -> "بروزرسانی آماده نصب است"
        is UpdateState.UpToDate -> "برنامه بروز است"
        is UpdateState.Error -> message
    }

    private fun Throwable.toPersianMessage(): String = when {
        message?.contains("timeout", ignoreCase = true) == true -> "زمان اتصال به سرور اشتراک به پایان رسید"
        message?.contains("host", ignoreCase = true) == true -> "آدرس اشتراک معتبر نیست"
        message?.contains("certificate", ignoreCase = true) == true -> "گواهی امنیتی سرور معتبر نیست"
        else -> "دریافت اشتراک ناموفق بود: ${message ?: "خطای ناشناخته"}"
    }

    private companion object {
        const val TAG = "KelavsVPN/ViewModel"
        const val STATS_INTERVAL_MS = 1_000L
    }
}

/**
 * Immutable UI snapshot.
 */
data class UiState(
    val tunnelState: TunnelState = TunnelState.DOWN,
    val stats: TrafficStats = TrafficStats(),
    val selectedServer: ServerConfigEntity? = null,
    val subscription: SubscriptionEntity? = null,
    val subscriptionUrl: String? = null,
    val settings: com.kelavs.vpn.data.SettingsSnapshot = com.kelavs.vpn.data.SettingsSnapshot(),
    val onboardingRequired: Boolean = false,
    val isImporting: Boolean = false,
    val updateStatus: String? = null,
    val downloadProgress: DownloadProgress? = null,
    val error: String? = null,
    val message: String? = null
) {
    val rxText: String get() = formatBytes(stats.rxBytes)
    val txText: String get() = formatBytes(stats.txBytes)
}
