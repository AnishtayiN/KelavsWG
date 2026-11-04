/*
 * Copyright © 2026 Kelavs VPN Team. All Rights Reserved.
 * SPDX-License-Identifier: Apache-2.0
 */
package com.kelavs.vpn.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * User preferences backed by [EncryptedSharedPreferences] (AES256-GCM via Android Keystore).
 *
 * Everything that could identify the user's subscription is stored encrypted: subscription URL,
 * server keys and the last used endpoint.
 */
class SecureSettings(context: Context) {

    private val prefs: SharedPreferences = createPrefs(context.applicationContext)

    private val _state = MutableStateFlow(readSnapshot())
    val state: StateFlow<SettingsSnapshot> = _state.asStateFlow()

    private fun createPrefs(context: Context): SharedPreferences = try {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    } catch (t: Throwable) {
        // KeyStore can be unavailable on broken ROMs; fall back to private prefs instead of crashing.
        Log.e(TAG, "Encrypted storage unavailable, falling back to private prefs", t)
        context.getSharedPreferences(FILE_NAME + "_fallback", Context.MODE_PRIVATE)
    }

    private fun readSnapshot() = SettingsSnapshot(
        darkTheme = prefs.getBoolean(KEY_DARK_THEME, true),
        usePersian = prefs.getBoolean(KEY_USE_PERSIAN, true),
        ipv6Enabled = prefs.getBoolean(KEY_IPV6, false),
        autoReconnect = prefs.getBoolean(KEY_AUTO_RECONNECT, true),
        autoUpdateCheck = prefs.getBoolean(KEY_AUTO_UPDATE, true),
        onboardingDone = prefs.getBoolean(KEY_ONBOARDING_DONE, false)
    )

    private fun update(block: (SharedPreferences.Editor) -> Unit) {
        val editor = prefs.edit()
        block(editor)
        editor.apply()
        _state.value = readSnapshot()
    }

    fun setDarkTheme(value: Boolean) = update { it.putBoolean(KEY_DARK_THEME, value) }
    fun setUsePersian(value: Boolean) = update { it.putBoolean(KEY_USE_PERSIAN, value) }
    fun setIpv6Enabled(value: Boolean) = update { it.putBoolean(KEY_IPV6, value) }
    fun setAutoReconnect(value: Boolean) = update { it.putBoolean(KEY_AUTO_RECONNECT, value) }
    fun setAutoUpdateCheck(value: Boolean) = update { it.putBoolean(KEY_AUTO_UPDATE, value) }
    fun setOnboardingDone(value: Boolean) = update { it.putBoolean(KEY_ONBOARDING_DONE, value) }

    var subscriptionUrl: String?
        get() = prefs.getString(KEY_SUBSCRIPTION_URL, null)
        set(value) = update { it.putString(KEY_SUBSCRIPTION_URL, value) }

    var lastSelectedPublicKey: String?
        get() = prefs.getString(KEY_LAST_SERVER, null)
        set(value) = update { it.putString(KEY_LAST_SERVER, value) }

    var lastConnectedAt: Long
        get() = prefs.getLong(KEY_LAST_CONNECTED, 0L)
        set(value) = update { it.putLong(KEY_LAST_CONNECTED, value) }

    private companion object {
        const val TAG = "KelavsVPN/Settings"
        const val FILE_NAME = "kelavs_secure_settings"

        const val KEY_DARK_THEME = "dark_theme"
        const val KEY_USE_PERSIAN = "use_persian"
        const val KEY_IPV6 = "ipv6_enabled"
        const val KEY_AUTO_RECONNECT = "auto_reconnect"
        const val KEY_AUTO_UPDATE = "auto_update"
        const val KEY_ONBOARDING_DONE = "onboarding_done"
        const val KEY_SUBSCRIPTION_URL = "subscription_url"
        const val KEY_LAST_SERVER = "last_server"
        const val KEY_LAST_CONNECTED = "last_connected_at"
    }
}

data class SettingsSnapshot(
    val darkTheme: Boolean = true,
    val usePersian: Boolean = true,
    val ipv6Enabled: Boolean = false,
    val autoReconnect: Boolean = true,
    val autoUpdateCheck: Boolean = true,
    val onboardingDone: Boolean = false
)
