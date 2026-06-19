package io.squarescreen.player.internal

import android.content.Context

private const val PREFS_FILE = "squarescreen_pairing"
private const val KEY_PAIRING_TOKEN = "pairing_token"

internal class PairingTokenStore(context: Context) {
    private val prefs = context.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)

    fun get(): String? = prefs.getString(KEY_PAIRING_TOKEN, null)

    fun save(token: String) = prefs.edit().putString(KEY_PAIRING_TOKEN, token).apply()

    fun clear() = prefs.edit().remove(KEY_PAIRING_TOKEN).apply()
}
