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
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Authentication token manager for Moodle APIs.
 * Uses EncryptedSharedPreferences for secure storage of sensitive data.
 * The Keystore-backed prefs are created lazily: call [ensureLoaded] off the main thread
 * before reading [isLoggedIn] or [getTokenSync].
 */
@Singleton
class TokenManager @Inject constructor(
    application: Application,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : TokenProvider {

    companion object {
        private const val PREFS_NAME = "webeep_secure_prefs"
        private const val KEY_WS_TOKEN = "ws_token"
        // Legacy key: the autologin-capable private token is no longer stored.
        private const val KEY_PRIVATE_TOKEN = "private_token"
        private const val KEY_USER_ID = "user_id"
        private const val KEY_FULL_NAME = "full_name"
        // Legacy key: single passport without timestamp, ignored and purged on load.
        private const val KEY_PENDING_PASSPORT = "pending_sso_passport"
        private const val KEY_PENDING_PASSPORTS = "pending_sso_passports"
        private const val USER_ID_DEFAULT = -1
    }

    private val encryptedPrefs: SharedPreferences by lazy { createEncryptedPrefsWithFallback(application) }

    private val loaded = AtomicBoolean(false)

    private val _isLoggedIn = MutableStateFlow(false)
    private val _wsToken = MutableStateFlow<String?>(null)

    override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()
    override val wsToken: Flow<String?> = _wsToken.asStateFlow()

    @Volatile
    private var cachedWsToken: String? = null

    private val clearLock = Any()

    override suspend fun ensureLoaded() {
        if (loaded.get()) return
        withContext(ioDispatcher) { loadIfNeeded() }
    }

    private fun loadIfNeeded() {
        if (loaded.get()) return
        synchronized(clearLock) {
            if (loaded.get()) return
            purgeLegacyKeys()
            refreshCacheFromPrefs()
            loaded.set(true)
        }
    }

    private fun purgeLegacyKeys() {
        if (encryptedPrefs.contains(KEY_PRIVATE_TOKEN) || encryptedPrefs.contains(KEY_PENDING_PASSPORT)) {
            encryptedPrefs.edit()
                .remove(KEY_PRIVATE_TOKEN)
                .remove(KEY_PENDING_PASSPORT)
                .commit()
        }
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

    /** [privateToken] is intentionally ignored: it grants a full web session and is never used. */
    override suspend fun saveAuthData(
        token: String,
        privateToken: String?,
        userId: Int,
        fullName: String
    ) {
        withContext(ioDispatcher) {
            synchronized(clearLock) {
                val success = encryptedPrefs.edit()
                    .putString(KEY_WS_TOKEN, token)
                    .putInt(KEY_USER_ID, userId)
                    .putString(KEY_FULL_NAME, fullName)
                    .remove(KEY_PRIVATE_TOKEN)
                    .remove(KEY_PENDING_PASSPORT)
                    .remove(KEY_PENDING_PASSPORTS)
                    .commit()
                if (!success) {
                    throw SecurityException("Impossibile persistere i dati di autenticazione in modo sicuro")
                }
                refreshCacheFromPrefs()
                loaded.set(true)
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
            loadIfNeeded()
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
            loaded.set(true)
        }
    }

    override fun savePendingPassportSync(passport: String) {
        synchronized(clearLock) {
            val now = System.currentTimeMillis()
            val entries = PendingPassports.add(readPassportEntries(now), passport, now)
            encryptedPrefs.edit()
                .remove(KEY_PENDING_PASSPORT)
                .putString(KEY_PENDING_PASSPORTS, PendingPassports.serialize(entries))
                .commit()
        }
    }

    override fun getPendingPassportSync(): String? {
        synchronized(clearLock) {
            return readPassportList().lastOrNull()
        }
    }

    override fun getPendingPassportsSync(): List<String> {
        synchronized(clearLock) {
            return readPassportList()
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

    private fun readPassportList(): List<String> =
        readPassportEntries(System.currentTimeMillis()).map { it.passport }

    private fun readPassportEntries(nowMs: Long): List<PendingPassports.Entry> =
        PendingPassports.parse(encryptedPrefs.getString(KEY_PENDING_PASSPORTS, null), nowMs)

    private fun clearAllInternal() {
        synchronized(clearLock) {
            encryptedPrefs.edit().clear().commit()
            refreshCacheFromPrefs()
            loaded.set(true)
        }
    }

    private fun refreshCacheFromPrefs() {
        val token = encryptedPrefs.getString(KEY_WS_TOKEN, null)
        cachedWsToken = token
        _wsToken.value = token
        _isLoggedIn.value = token != null
    }
}

/**
 * Pending SSO passports stored as `passport|createdAtMillis` lines.
 * Entries older than [TTL_MS] (or with an invalid or future timestamp) are discarded on read.
 */
internal object PendingPassports {
    const val TTL_MS = 10 * 60_000L
    const val MAX_ENTRIES = 8

    data class Entry(val passport: String, val createdAtMs: Long)

    fun parse(raw: String?, nowMs: Long): List<Entry> {
        if (raw.isNullOrBlank()) return emptyList()
        return raw.split('\n').mapNotNull { line ->
            val passport = line.substringBefore('|').trim()
            val createdAt = line.substringAfter('|', "").trim().toLongOrNull()
            if (passport.isEmpty() || createdAt == null) return@mapNotNull null
            val age = nowMs - createdAt
            if (age < 0 || age >= TTL_MS) null else Entry(passport, createdAt)
        }
    }

    fun add(entries: List<Entry>, passport: String, nowMs: Long): List<Entry> =
        (entries.filterNot { it.passport == passport } + Entry(passport, nowMs)).takeLast(MAX_ENTRIES)

    fun serialize(entries: List<Entry>): String =
        entries.joinToString("\n") { "${it.passport}|${it.createdAtMs}" }
}
