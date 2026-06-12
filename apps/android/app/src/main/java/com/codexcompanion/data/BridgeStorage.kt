package com.codexcompanion.data

import android.content.Context
import com.codexcompanion.model.SavedBridge

class BridgeStorage(context: Context) {
    private val prefs = context.getSharedPreferences("codex_companion", Context.MODE_PRIVATE)

    fun load(): SavedBridge? {
        val baseUrl = prefs.getString("baseUrl", null) ?: return null
        val bridgeName = prefs.getString("bridgeName", null) ?: return null
        val deviceId = prefs.getString("deviceId", null) ?: return null
        val authToken = prefs.getString("authToken", null) ?: return null
        val networkMode = prefs.getString("networkMode", null) ?: "lan"
        val lanUrl = prefs.getString("lanUrl", null) ?: baseUrl
        val remoteUrl = prefs.getString("remoteUrl", null) ?: ""
        return SavedBridge(baseUrl, bridgeName, deviceId, authToken, networkMode, lanUrl, remoteUrl)
    }

    fun save(bridge: SavedBridge) {
        prefs.edit()
            .putString("baseUrl", bridge.baseUrl)
            .putString("bridgeName", bridge.bridgeName)
            .putString("deviceId", bridge.deviceId)
            .putString("authToken", bridge.authToken)
            .putString("networkMode", bridge.networkMode)
            .putString("lanUrl", bridge.lanUrl)
            .putString("remoteUrl", bridge.remoteUrl)
            .apply()
    }

    fun clear() {
        prefs.edit().clear().apply()
    }
}
