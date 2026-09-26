package it.polimi.webeep.core.datastore

import android.content.Context
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.File

/**
 * One-shot import of the session stored by the deprecated EncryptedSharedPreferences.
 * Legacy migration, remove after next release (together with the security-crypto dependency).
 */
internal object LegacyEncryptedPrefsMigration {

    private const val TAG = "LegacyPrefsMigration"
    private const val PREFS_NAME = "webeep_secure_prefs"
    private const val KEY_WS_TOKEN = "ws_token"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_FULL_NAME = "full_name"
    private const val KEY_PENDING_PASSPORTS = "pending_sso_passports"
    private const val USER_ID_DEFAULT = -1

    fun exists(context: Context): Boolean =
        File(context.dataDir, "shared_prefs/$PREFS_NAME.xml").exists()

    /** Returns the legacy session, or null if the file cannot be decrypted. */
    fun read(context: Context): AuthRecord? = try {
        // Legacy migration, remove after next release: security-crypto is used only here.
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val prefs = EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
        AuthRecord(
            token = prefs.getString(KEY_WS_TOKEN, null),
            userId = prefs.getInt(KEY_USER_ID, USER_ID_DEFAULT).takeIf { it != USER_ID_DEFAULT },
            fullName = prefs.getString(KEY_FULL_NAME, null),
            passports = prefs.getString(KEY_PENDING_PASSPORTS, null)
        )
    } catch (_: Exception) {
        Log.w(TAG, "Legacy secure prefs unreadable, discarding them")
        null
    }

    fun delete(context: Context) {
        try {
            context.deleteSharedPreferences(PREFS_NAME)
        } catch (_: Exception) {
            Log.w(TAG, "Unable to delete legacy secure prefs")
        }
    }
}
