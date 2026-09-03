package it.polimi.webeep.core.datastore

import android.app.Application
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import it.polimi.webeep.core.di.IoDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication token manager for Moodle APIs.
 * Uses EncryptedSharedPreferences for secure storage of sensitive data.
 */
@Singleton
class TokenManager @Inject constructor(
    application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TokenProvider {

    companion object {
        private const val PREFS_NAME = "webeep_secure_prefs"
        private const val KEY_WS_TOKEN = "ws_token"
        private const val KEY_PRIVATE_TOKEN = "private_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        private const val KEY_PENDING_PASSPORT = "pending_sso_passport"
        private const val KEY_PENDING_PASSPORTS = "pending_sso_passports"
        private const val MAX_PENDING_PASSPORTS = 8
        private const val USER_ID_DEFAULT = -1
    }

    private val encryptedPrefs: SharedPreferences

    private val _isLoggedIn = MutableStateFlow(false)
    private val _wsToken = MutableStateFlow<String?>(null)

    override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()
    override val wsToken: Flow<String?> = _wsToken.asStateFlow()

    @Volatile
    private var cachedWsToken: String? = null

    private val clearLock = Any()

    init {
        encryptedPrefs = createEncryptedPrefsWithFallback(application)
        refreshCacheFromPrefs()
    }

    private fun createEncryptedPrefsWithFallback(application: Application): SharedPreferences {
        return try {
            createEncryptedPrefs(application)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            // First retry without StrongBox and WITHOUT wiping: covers devices
            // without StrongBox or with a transient keystore error. Wipe only
            // as last resort for actually corrupted prefs.
            try {
                return createEncryptedPrefs(application, forceFallbackKey = true)
            } catch (fallbackError: Exception) {
                if (fallbackError is CancellationException) throw fallbackError
                android.util.Log.w("TokenManager", "Encrypted prefs corrupted, resetting secure storage")
                try {
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                        application.deleteSharedPreferences(PREFS_NAME)
                    } else {
                        java.io.File(application.applicationInfo.dataDir, "shared_prefs/$PREFS_NAME.xml").delete()
                    }
                } catch (_: Exception) {}
                return createEncryptedPrefs(application, forceFallbackKey = true)
            }
        }
    }

    private fun createEncryptedPrefs(application: Application, forceFallbackKey: Boolean = false): SharedPreferences {
        val masterKey = createMasterKey(application, forceFallback = forceFallbackKey)
        return EncryptedSharedPreferences.create(
            application,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private fun createMasterKey(application: Application, forceFallback: Boolean = false): MasterKey {
        return if (!forceFallback) {
            try {
                MasterKey.Builder(application)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .setRequestStrongBoxBacked(true)
                    .build()
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                MasterKey.Builder(application)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build()
            }
        } else {
            MasterKey.Builder(application)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }
    }

    override fun getTokenSync(): String? = cachedWsToken

    override suspend fun saveAuthData(
        token: String,
        privateToken: String?,
        userId: Int,
        fullName: String
    ) {
        withContext(ioDispatcher) {
            synchronized(clearLock) {
                val editor = encryptedPrefs.edit()
                    .putString(KEY_WS_TOKEN, token)
                    .putInt(KEY_USER_ID, userId)
                    .putString(KEY_FULL_NAME, fullName)
                    .remove(KEY_PENDING_PASSPORT)
                    .remove(KEY_PENDING_PASSPORTS)
                if (privateToken != null) {
                    editor.putString(KEY_PRIVATE_TOKEN, privateToken)
                }
                val success = editor.commit()
                if (!success) {
                    throw SecurityException("Impossibile persistere i dati di autenticazione in modo sicuro")
                }
                refreshCacheFromPrefs()
            }
        }
    }

    override suspend fun updateUserProfile(userId: Int, fullName: String) {
        withContext(ioDispatcher) {
            synchronized(clearLock) {
                val success = encryptedPrefs.edit()
                    .putInt(KEY_USER_ID, userId)
                    .putString(KEY_FULL_NAME, fullName)
                    .commit()
                if (!success) {
                    throw SecurityException("Impossibile aggiornare il profilo utente")
                }
            }
        }
    }

    override suspend fun getToken(): String? {
        return withContext(ioDispatcher) {
            synchronized(clearLock) {
                encryptedPrefs.getString(KEY_WS_TOKEN, null).also { cachedWsToken = it }
            }
        }
    }

    override suspend fun getUserId(): Int? {
        return withContext(ioDispatcher) {
            synchronized(clearLock) {
                val userId = encryptedPrefs.getInt(KEY_USER_ID, USER_ID_DEFAULT)
                if (userId == USER_ID_DEFAULT) null else userId
            }
        }
    }

    override suspend fun getUserFullName(): String? {
        return withContext(ioDispatcher) {
            synchronized(clearLock) {
                encryptedPrefs.getString(KEY_FULL_NAME, null)
            }
        }
    }

    override suspend fun clearAll() {
        withContext(ioDispatcher) {
            clearAllInternal()
        }
    }

    override fun clearSessionSync() {
        synchronized(clearLock) {
            encryptedPrefs.edit().clear().commit()
            cachedWsToken = null
            _wsToken.value = null
            _isLoggedIn.value = false
        }
    }

    override fun savePendingPassportSync(passport: String) {
        synchronized(clearLock) {
            val passports = readPassportList().toMutableList()
            passports.remove(passport)
            passports.add(passport)
            while (passports.size > MAX_PENDING_PASSPORTS) {
                passports.removeAt(0)
            }
            encryptedPrefs.edit()
                .putString(KEY_PENDING_PASSPORT, passport)
                .putString(KEY_PENDING_PASSPORTS, passports.joinToString("\n"))
                .commit()
        }
    }

    override fun getPendingPassportSync(): String? {
        synchronized(clearLock) {
            return readPassportList().lastOrNull()
                ?: encryptedPrefs.getString(KEY_PENDING_PASSPORT, null)
        }
    }

    override fun getPendingPassportsSync(): List<String> {
        synchronized(clearLock) {
            val list = readPassportList()
            if (list.isNotEmpty()) return list
            return listOfNotNull(encryptedPrefs.getString(KEY_PENDING_PASSPORT, null))
        }
    }

    override fun clearPendingPassportSync() {
        synchronized(clearLock) {
            encryptedPrefs.edit()
                .remove(KEY_PENDING_PASSPORT)
                .remove(KEY_PENDING_PASSPORTS)
                .commit()
        }
    }

    private fun readPassportList(): List<String> {
        return encryptedPrefs.getString(KEY_PENDING_PASSPORTS, null)
            ?.split('\n')
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()
    }

    private fun clearAllInternal() {
        synchronized(clearLock) {
            encryptedPrefs.edit().clear().commit()
            refreshCacheFromPrefs()
        }
    }

    private fun refreshCacheFromPrefs() {
        val token = encryptedPrefs.getString(KEY_WS_TOKEN, null)
        cachedWsToken = token
        _wsToken.value = token
        _isLoggedIn.value = token != null
    }
}
