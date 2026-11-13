package it.polimi.webeep.core.datastore

import android.content.Context
import android.util.Log
import java.security.KeyStore

/**
 * Removes what the pre-1.1.0 EncryptedSharedPreferences store left on the device.
 *
 * The old session is no longer imported (users upgrading from before 1.1.0 sign in again), but the
 * file is still deleted so an encrypted copy of a token is never kept for no reason.
 */
internal object LegacySecurePrefsCleanup {

    private const val TAG = "LegacyPrefsCleanup"
    private const val PREFS_NAME = "webeep_secure_prefs"
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"

    /** Default alias of the master key that `androidx.security.crypto.MasterKey` created; not the current `webeep_master_key`. */
    private const val LEGACY_MASTER_KEY_ALIAS = "_androidx_security_master_key_"

    /** Deletes the legacy preferences file and its Keystore master key; failures are logged and ignored. */
    fun run(context: Context) {
        try {
            context.deleteSharedPreferences(PREFS_NAME)
        } catch (_: Exception) {
            Log.w(TAG, "Unable to delete legacy secure prefs")
        }
        try {
            val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
            if (keyStore.containsAlias(LEGACY_MASTER_KEY_ALIAS)) keyStore.deleteEntry(LEGACY_MASTER_KEY_ALIAS)
        } catch (_: Exception) {
            Log.w(TAG, "Unable to delete legacy master key")
        }
    }
}
