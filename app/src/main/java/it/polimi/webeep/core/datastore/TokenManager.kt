package it.polimi.webeep.core.datastore

import android.app.Application
import android.util.Log
import it.polimi.webeep.core.di.IoDispatcher
import it.polimi.webeep.core.util.runCatchingCancellable
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * Authentication token manager for Moodle APIs.
 * Credentials are persisted through [SecureStore] (Tink AEAD + Android Keystore, DataStore file
 * excluded from backups) and mirrored in memory. The store is opened lazily: call [ensureLoaded]
 * off the main thread before reading [isLoggedIn] or [getTokenSync].
 */
@Singleton
class TokenManager @Inject constructor(private val application: Application, @IoDispatcher private val ioDispatcher: CoroutineDispatcher) :
    TokenProvider {

    companion object {
        private const val TAG = "TokenManager"
    }

    private val store: SecureStore by lazy { SecureStore(application) }

    private val loaded = AtomicBoolean(false)

    private val _isLoggedIn = MutableStateFlow(false)
    private val _wsToken = MutableStateFlow<String?>(null)

    override val isLoggedIn: Flow<Boolean> = _isLoggedIn.asStateFlow()
    override val wsToken: Flow<String?> = _wsToken.asStateFlow()

    @Volatile
    private var cachedWsToken: String? = null

    private val clearLock = Any()

    // In-memory copy of the persisted data, guarded by [clearLock]. Valid once [loaded] is true.
    private var record = AuthRecord.EMPTY

    override val isLoaded: Boolean get() = loaded.get()

    override suspend fun ensureLoaded() {
        if (loaded.get()) return
        withContext(ioDispatcher) { loadIfNeeded() }
    }

    private fun loadIfNeeded() {
        if (loaded.get()) return
        synchronized(clearLock) {
            if (loaded.get()) return
            record = readPersistedRecord()
            publish()
            loaded.set(true)
        }
    }

    private fun readPersistedRecord(): AuthRecord {
        val current = AuthRecord.fromMap(store.readAll())
        if (!LegacyEncryptedPrefsMigration.exists(application)) return current
        // Legacy migration, remove after next release.
        val legacy = LegacyEncryptedPrefsMigration.read(application)
        if (legacy == null || legacy.isEmpty() || !current.isEmpty()) {
            LegacyEncryptedPrefsMigration.delete(application)
            return current
        }
        // Keep the legacy file when the new store cannot persist, so the next launch retries.
        if (persist(legacy)) LegacyEncryptedPrefsMigration.delete(application)
        return legacy
    }

    override fun getTokenSync(): String? = cachedWsToken

    override suspend fun saveAuthData(token: String, userId: Int, fullName: String) {
        withContext(ioDispatcher) {
            synchronized(clearLock) {
                val updated = AuthRecord(token = token, userId = userId, fullName = fullName)
                if (!persist(updated)) {
                    throw SecurityException("Impossibile persistere i dati di autenticazione in modo sicuro")
                }
                record = updated
                publish()
                loaded.set(true)
            }
        }
    }

    override suspend fun updateUserProfile(userId: Int, fullName: String) {
        withContext(ioDispatcher) {
            synchronized(clearLock) {
                loadIfNeeded()
                val updated = record.copy(userId = userId, fullName = fullName)
                if (!persist(updated)) {
                    throw SecurityException("Impossibile aggiornare il profilo utente")
                }
                record = updated
            }
        }
    }

    override suspend fun getToken(): String? = withContext(ioDispatcher) {
        loadIfNeeded()
        synchronized(clearLock) {
            record.token.also { cachedWsToken = it }
        }
    }

    override suspend fun getUserId(): Int? = withContext(ioDispatcher) {
        loadIfNeeded()
        synchronized(clearLock) { record.userId }
    }

    override suspend fun clearAll() {
        withContext(ioDispatcher) {
            clearInternal()
        }
    }

    override fun clearSessionSync() {
        clearInternal()
    }

    override fun savePendingPassportSync(passport: String) {
        synchronized(clearLock) {
            loadIfNeeded()
            val now = System.currentTimeMillis()
            val entries = PendingPassports.add(PendingPassports.parse(record.passports, now), passport, now)
            record = record.copy(passports = PendingPassports.serialize(entries))
            persist(record)
        }
    }

    override fun getPendingPassportsSync(): List<String> {
        synchronized(clearLock) {
            return readPassportList()
        }
    }

    override fun clearPendingPassportSync() {
        synchronized(clearLock) {
            loadIfNeeded()
            record = record.copy(passports = null)
            persist(record)
        }
    }

    private fun readPassportList(): List<String> {
        loadIfNeeded()
        return PendingPassports.parse(record.passports, System.currentTimeMillis()).map { it.passport }
    }

    private fun clearInternal() {
        synchronized(clearLock) {
            record = AuthRecord.EMPTY
            persist(record)
            LegacyEncryptedPrefsMigration.delete(application)
            publish()
            loaded.set(true)
        }
    }

    /** Writes [updated] to the secure store; never falls back to cleartext. */
    private fun persist(updated: AuthRecord): Boolean = runCatchingCancellable { store.replaceAll(updated.toMap()) }
        .onFailure { Log.w(TAG, "Unable to persist secure session data") }
        .isSuccess

    private fun publish() {
        val token = record.token
        cachedWsToken = token
        _wsToken.value = token
        _isLoggedIn.value = token != null
    }
}

/** Persisted session fields; stored as individual encrypted string values. */
internal data class AuthRecord(
    val token: String? = null,
    val userId: Int? = null,
    val fullName: String? = null,
    val passports: String? = null
) {
    fun isEmpty(): Boolean = toMap().isEmpty()

    fun toMap(): Map<String, String> = buildMap {
        token?.let { put(KEY_WS_TOKEN, it) }
        userId?.let { put(KEY_USER_ID, it.toString()) }
        fullName?.let { put(KEY_FULL_NAME, it) }
        passports?.takeIf { it.isNotEmpty() }?.let { put(KEY_PENDING_PASSPORTS, it) }
    }

    // Never leak credentials through logs or crash reports.
    override fun toString(): String = "AuthRecord(token=${if (token != null) "***" else null}, userId=$userId, " +
        "fullName=${if (fullName != null) "***" else null}, passports=${if (passports != null) "***" else null})"

    companion object {
        const val KEY_WS_TOKEN = "ws_token"
        const val KEY_USER_ID = "user_id"
        const val KEY_FULL_NAME = "full_name"
        const val KEY_PENDING_PASSPORTS = "pending_sso_passports"
        private const val USER_ID_DEFAULT = -1

        val EMPTY = AuthRecord()

        fun fromMap(values: Map<String, String>): AuthRecord = AuthRecord(
            token = values[KEY_WS_TOKEN],
            userId = values[KEY_USER_ID]?.toIntOrNull()?.takeIf { it != USER_ID_DEFAULT },
            fullName = values[KEY_FULL_NAME],
            passports = values[KEY_PENDING_PASSPORTS]
        )
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

    fun serialize(entries: List<Entry>): String = entries.joinToString("\n") { "${it.passport}|${it.createdAtMs}" }
}
