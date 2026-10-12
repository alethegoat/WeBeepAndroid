package it.polimi.webeep.core.datastore

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.RegistryConfiguration
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import java.io.File
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.util.Base64
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

/**
 * Encrypted key-value store for credentials.
 *
 * Values are sealed with a Tink AES256-GCM [Aead] (associated data = key name, so ciphertexts
 * cannot be swapped between keys). The Tink keyset lives in a private SharedPreferences file,
 * wrapped by an Android Keystore master key. Ciphertexts live in a DataStore file under
 * noBackupFilesDir. Nothing is ever written in clear: if Keystore/Tink cannot be used even after
 * a reset, the store refuses writes.
 *
 * All calls are blocking; callers are expected to serialize them.
 */
internal class SecureStore(context: Context) {

    private val appContext: Context = context.applicationContext
    private val dataStore: DataStore<Preferences> = dataStoreFor(appContext)

    // Null when Keystore/Tink is unusable even after a reset.
    private val aead: Aead? by lazy { openAead() }

    fun readAll(): Map<String, String> {
        val aead = aead ?: return emptyMap()
        val prefs = try {
            runBlocking { dataStore.data.first() }
        } catch (_: Exception) {
            Log.w(TAG, "Secure store unreadable")
            return emptyMap()
        }
        val result = HashMap<String, String>()
        for ((key, value) in prefs.asMap()) {
            val sealed = value as? String ?: continue
            SecureValueCodec.open(aead, key.name, sealed)?.let { result[key.name] = it }
        }
        return result
    }

    /** Atomically replaces the whole content with [values]. Throws if this cannot be done securely. */
    fun replaceAll(values: Map<String, String>) {
        if (values.isEmpty()) {
            clear()
            return
        }
        val aead = aead ?: throw GeneralSecurityException("Secure storage unavailable")
        val sealed = values.mapValues { (key, value) -> SecureValueCodec.seal(aead, key, value) }
        runBlocking {
            dataStore.edit { prefs ->
                prefs.clear()
                sealed.forEach { (key, value) -> prefs[stringPreferencesKey(key)] = value }
            }
        }
    }

    fun clear() {
        runBlocking { dataStore.edit { it.clear() } }
    }

    private fun openAead(): Aead? {
        try {
            return createAead()
        } catch (_: Exception) {
            Log.w(TAG, "Secure keyset unusable, resetting secure storage")
        }
        // Old ciphertexts are unreadable without the old keyset: wipe everything and start over.
        resetKeyMaterial()
        return try {
            createAead()
        } catch (_: Exception) {
            Log.w(TAG, "Android Keystore unavailable, credentials will not be persisted")
            resetKeyMaterial()
            null
        }
    }

    private fun createAead(): Aead {
        AeadConfig.register()
        val manager = AndroidKeysetManager.Builder()
            .withSharedPref(appContext, KEYSET_NAME, KEYSET_PREFS_NAME)
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri(MASTER_KEY_URI)
            .build()
        // AndroidKeysetManager silently falls back to a cleartext keyset when Keystore fails.
        if (!manager.isUsingKeystore()) {
            throw GeneralSecurityException("Keyset is not protected by Android Keystore")
        }
        return manager.keysetHandle.getPrimitive(RegistryConfiguration.get(), Aead::class.java)
    }

    private fun resetKeyMaterial() {
        runCatching { clear() }
        runCatching {
            appContext.getSharedPreferences(KEYSET_PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
            appContext.deleteSharedPreferences(KEYSET_PREFS_NAME)
        }
        runCatching {
            KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }.deleteEntry(MASTER_KEY_ALIAS)
        }
    }

    companion object {
        private const val TAG = "SecureStore"
        private const val FILE_NAME = "webeep_secure.preferences_pb"
        private const val KEYSET_NAME = "webeep_secure_keyset"
        private const val KEYSET_PREFS_NAME = "webeep_secure_keyset_prefs"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MASTER_KEY_ALIAS = "webeep_master_key"
        private const val MASTER_KEY_URI = "android-keystore://$MASTER_KEY_ALIAS"

        @Volatile
        private var sharedDataStore: DataStore<Preferences>? = null

        // DataStore allows a single active instance per file in a process.
        private fun dataStoreFor(context: Context): DataStore<Preferences> = sharedDataStore ?: synchronized(this) {
            sharedDataStore ?: PreferenceDataStoreFactory.create(
                corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                produceFile = { File(context.noBackupFilesDir, FILE_NAME) }
            ).also { sharedDataStore = it }
        }
    }
}

/** Seals single values with the key name as associated data; Base64 for string storage. */
internal object SecureValueCodec {

    fun seal(aead: Aead, key: String, value: String): String {
        val ciphertext = aead.encrypt(value.toByteArray(Charsets.UTF_8), key.toByteArray(Charsets.UTF_8))
        return Base64.getEncoder().encodeToString(ciphertext)
    }

    /** Returns null when [sealed] is malformed, tampered with or bound to another key. */
    fun open(aead: Aead, key: String, sealed: String): String? = try {
        val plaintext = aead.decrypt(Base64.getDecoder().decode(sealed), key.toByteArray(Charsets.UTF_8))
        String(plaintext, Charsets.UTF_8)
    } catch (_: GeneralSecurityException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }
}
