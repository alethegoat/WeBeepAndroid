package it.polimi.webeep.core.datastore

import android.app.Application
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * authentication token manager for moodle apis.
 * uses encryptedsharedpreferences for secure storage of sensitive data.
 * implements [tokenprovider] to allow fake injection in tests.
 */
@Singleton
class TokenManager @Inject constructor(application: Application) : TokenProvider {

    companion object {
        private const val PREFS_NAME = "webeep_secure_prefs"
        private const val KEY_WS_TOKEN = "ws_token"
        private const val KEY_PRIVATE_TOKEN = "private_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        private const val USER_ID_DEFAULT = -1
    }

    private val encryptedPrefs: SharedPreferences

    private val _isLoggedIn = MutableStateFlow(false)
    private val _wsToken = MutableStateFlow<String?>(null)

    /** flow emitting user authentication state. */
    override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()

    /** current ws token (null if not authenticated). used to combine offline cache. */
    override val wsToken: Flow<String?> = _wsToken.asStateFlow()

    @Volatile
    private var cachedWsToken: String? = null

    private val clearLock = Any()

    init {
        val masterKey = createMasterKey(application)

        encryptedPrefs = EncryptedSharedPreferences.create(
            application,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )

        refreshCacheFromPrefs()
    }

    /**
     * creates masterkey with strongbox (hardware-backed se) support where available.
     * automatically falls back to standard tee if strongbox is not supported.
     */
    private fun createMasterKey(application: Application): MasterKey {
        return try {
            MasterKey.Builder(application)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .setRequestStrongBoxBacked(true)
                .build()
        } catch (e: Exception) {
            // fallback to standard tee if strongbox is not available
            MasterKey.Builder(application)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        }
    }

    /** synchronous token read for okhttp (thread-safe, without runblocking). */
    override fun getTokenSync(): String? = cachedWsToken

    override suspend fun saveAuthData(
        token: String,
        privateToken: String?,
        userId: Int,
        fullName: String
    ) {
        withContext(Dispatchers.IO) {
            val success = encryptedPrefs.edit()
                .putString(KEY_WS_TOKEN, token)
                .putString(KEY_PRIVATE_TOKEN, privateToken)
                .putInt(KEY_USER_ID, userId)
                .putString(KEY_FULL_NAME, fullName)
                .commit()

            if (!success) {
                throw SecurityException("Impossibile persistere i dati di autenticazione in modo sicuro")
            }

            refreshCacheFromPrefs()
        }
    }

    override suspend fun getToken(): String? {
        return withContext(Dispatchers.IO) {
            encryptedPrefs.getString(KEY_WS_TOKEN, null).also { cachedWsToken = it }
        }
    }

    override suspend fun getUserId(): Int? {
        return withContext(Dispatchers.IO) {
            val userId = encryptedPrefs.getInt(KEY_USER_ID, USER_ID_DEFAULT)
            if (userId == USER_ID_DEFAULT) null else userId
        }
    }

    override suspend fun getUserFullName(): String? {
        return withContext(Dispatchers.IO) {
            encryptedPrefs.getString(KEY_FULL_NAME, null)
        }
    }

    override suspend fun clearAll() {
        withContext(Dispatchers.IO) {
            clearAllInternal()
        }
    }

    /** session invalidation from http interceptor — thread-safe with lock. */
    override fun clearSessionSync() {
        synchronized(clearLock) {
            encryptedPrefs.edit().clear().apply()
            cachedWsToken = null
            _wsToken.value = null
            _isLoggedIn.value = false
        }
    }

    private fun clearAllInternal() {
        synchronized(clearLock) {
            encryptedPrefs.edit().clear().apply()
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
