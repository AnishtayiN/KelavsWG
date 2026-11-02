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
               isValidBase64(privateKey) &&
               isValidBase64(peerPublicKey) &&
               (peerPresharedKey == null || peerPresharedKey!.isBlank() || isValidBase64(peerPresharedKey!)) &&
               isValidEndpoint(peerEndpoint)
    }

    private fun isValidBase64(str: String): Boolean {
        return try {
            android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun isValidEndpoint(endpoint: String): Boolean {
        // Basic endpoint validation: host:port
        val parts = endpoint.split(":")
        return parts.size == 2 && parts[0].isNotBlank() && parts[1].toIntOrNull() != null
    }
}