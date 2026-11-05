package com.kelavs.vpn.subscription

import android.os.Parcel
import android.os.Parcelable
import kotlinx.serialization.Serializable

/**
 * Represents a single WireGuard configuration parsed from a subscription.
 */
@Serializable
data class SubscriptionConfig(
    val name: String,
    val privateKey: String,
    val address: String,
    val dns: String?,
    val mtu: Int?,
    val persistentKeepalive: Int?,
    val peerPublicKey: String,
    val peerAllowedIPs: String,
    val peerEndpoint: String,
    val peerPresharedKey: String? = null,
    val originalConfig: String
) : Parcelable {

    constructor(parcel: Parcel) : this(
        name = parcel.readString()!!,
        privateKey = parcel.readString()!!,
        address = parcel.readString()!!,
        dns = parcel.readString(),
        mtu = if (parcel.readInt() == -1) null else parcel.readInt(),
        persistentKeepalive = if (parcel.readInt() == -1) null else parcel.readInt(),
        peerPublicKey = parcel.readString()!!,
        peerAllowedIPs = parcel.readString()!!,
        peerEndpoint = parcel.readString()!!,
        peerPresharedKey = parcel.readString(),
        originalConfig = parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(name)
        parcel.writeString(privateKey)
        parcel.writeString(address)
        parcel.writeString(dns)
        parcel.writeInt(mtu ?: -1)
        parcel.writeInt(persistentKeepalive ?: -1)
        parcel.writeString(peerPublicKey)
        parcel.writeString(peerAllowedIPs)
        parcel.writeString(peerEndpoint)
        parcel.writeString(peerPresharedKey)
        parcel.writeString(originalConfig)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<SubscriptionConfig> {
        override fun createFromParcel(parcel: Parcel): SubscriptionConfig = SubscriptionConfig(parcel)
        override fun newArray(size: Int): Array<SubscriptionConfig?> = arrayOfNulls(size)
    }

    /**
     * Converts this config to WireGuard Android's Config format.
     */
    fun toWireGuardConfig(): String = originalConfig

    /**
     * Validates that all required fields are present and well-formed.
     */
    fun isValid(): Boolean {
        return privateKey.isNotBlank() &&
               address.isNotBlank() &&
               peerPublicKey.isNotBlank() &&
               peerAllowedIPs.isNotBlank() &&
               peerEndpoint.isNotBlank() &&
               isValidKey(privateKey) &&
               isValidKey(peerPublicKey) &&
               isPresharedKeyValid() &&
               isValidEndpoint(peerEndpoint)
    }

    private fun isPresharedKeyValid(): Boolean {
        val key = peerPresharedKey
        return key.isNullOrBlank() || isValidKey(key)
    }

    /**
     * WireGuard keys are 32 bytes in Base64. Providers emit them with padding (44 chars),
     * without padding (43 chars) or with the URL-safe alphabet, so all three are accepted.
     */
    fun isValidKey(str: String): Boolean {
        val trimmed = str.trim()
        if (trimmed.length !in 42..44) return false
        val normalized = trimmed.replace('-', '+').replace('_', '/')
        val padded = when (normalized.length % 4) {
            0 -> normalized
            2 -> "$normalized=="
            3 -> "$normalized="
            else -> return false
        }
        return try {
            java.util.Base64.getDecoder().decode(padded).size == 32
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    /**
     * Accepts `host:port` and `[ipv6]:port`. IPv6 endpoints used to be rejected because a
     * naive split(':') produced more than two parts.
     */
    fun isValidEndpoint(endpoint: String): Boolean {
        val value = endpoint.trim()
        if (value.isEmpty()) return false
        val host: String
        val portText: String
        if (value.startsWith("[")) {
            val close = value.indexOf(']')
            if (close < 0) return false
            host = value.substring(1, close)
            portText = value.substring(close + 1).removePrefix(":")
        } else {
            val idx = value.lastIndexOf(':')
            if (idx <= 0) return false
            host = value.substring(0, idx)
            portText = value.substring(idx + 1)
        }
        if (host.isBlank() || host.any { it.isWhitespace() }) return false
        val port = portText.toIntOrNull() ?: return false
        return port in 1..65535
    }
}
