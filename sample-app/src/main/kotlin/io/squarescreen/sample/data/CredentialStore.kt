package io.squarescreen.sample.data

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

private const val PREFS_FILE = "squarescreen_credentials"
private const val KEY_DEVICE_ID = "device_id"
private const val KEY_DEVICE_TOKEN = "device_token"

/**
 * Secure local storage for device credentials.
 *
 * Credentials are encrypted at rest using AES-256-GCM via [EncryptedSharedPreferences].
 * The encryption key is stored in the Android Keystore — it never leaves the device.
 *
 * Why EncryptedSharedPreferences?
 * The device token is a long-lived secret that authenticates this device to the
 * SquareScreen API. If it were stored in plain SharedPreferences and the device
 * were rooted or the APK unpacked, the token could be extracted and used to
 * impersonate the device. Encryption at rest makes this significantly harder.
 */
class CredentialStore(context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            PREFS_FILE,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    /** Returns stored device credentials, or null if the device has not been paired. */
    fun getCredentials(): DeviceCredentials? {
        val deviceId = prefs.getString(KEY_DEVICE_ID, null) ?: return null
        val deviceToken = prefs.getString(KEY_DEVICE_TOKEN, null) ?: return null
        return DeviceCredentials(deviceId = deviceId, deviceToken = deviceToken)
    }

    /**
     * Persists device credentials after a successful registration or pairing.
     * Call this once after the server returns the device token.
     */
    fun saveCredentials(credentials: DeviceCredentials) {
        prefs.edit()
            .putString(KEY_DEVICE_ID, credentials.deviceId)
            .putString(KEY_DEVICE_TOKEN, credentials.deviceToken)
            .apply()
    }

    /** Clears stored credentials. Use to force re-pairing (e.g. factory reset). */
    fun clearCredentials() {
        prefs.edit().clear().apply()
    }

    /** Returns true if this device has been paired and credentials are stored. */
    fun isPaired(): Boolean = getCredentials() != null
}

/**
 * Immutable device credentials used to authenticate with the SquareScreen API.
 *
 * @param deviceId A unique identifier for this device (UUID). Stable across app restarts.
 * @param deviceToken A secret token issued by the SquareScreen backend during device registration.
 */
data class DeviceCredentials(
    val deviceId: String,
    val deviceToken: String
)
