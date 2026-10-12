package it.polimi.webeep.feature.sync.data

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.feature.settings.domain.SettingsRepository
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.sync.CourseSyncWorker
import it.polimi.webeep.feature.sync.domain.SyncScheduling
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class SyncScheduler internal constructor(
    private val settingsRepository: SettingsRepository,
    private val tokenProvider: TokenProvider,
    private val workManagerProvider: () -> WorkManager
) : SyncScheduling {
    @Inject
    constructor(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository,
        tokenProvider: TokenProvider
    ) : this(settingsRepository, tokenProvider, { WorkManager.getInstance(context) })

    private val workManager: WorkManager
        get() = workManagerProvider()

    override suspend fun applyCurrentSettings(replaceExisting: Boolean) {
        if (!hasSession()) {
            cancelAll()
            return
        }

        val frequency = settingsRepository.syncFrequency.first()
        if (frequency == SyncFrequency.NEVER) {
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
            return
        }

        val (interval, unit) = when (frequency) {
            SyncFrequency.FIFTEEN_MINUTES -> FIFTEEN_MINUTES_INTERVAL to TimeUnit.MINUTES
            SyncFrequency.ONE_HOUR -> 1L to TimeUnit.HOURS
            SyncFrequency.TWELVE_HOURS -> TWELVE_HOURS_INTERVAL to TimeUnit.HOURS
            SyncFrequency.DAILY -> 1L to TimeUnit.DAYS
            SyncFrequency.NEVER -> return
        }

        val request = PeriodicWorkRequestBuilder<CourseSyncWorker>(interval, unit)
            .setConstraints(networkConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_MINUTES,
                TimeUnit.MINUTES
            )
            .build()

        val policy = if (replaceExisting) {
            ExistingPeriodicWorkPolicy.UPDATE
        } else {
            ExistingPeriodicWorkPolicy.KEEP
        }
        workManager.enqueueUniquePeriodicWork(UNIQUE_PERIODIC_WORK, policy, request)
    }

    override suspend fun enqueueImmediate(manual: Boolean) {
        if (!hasSession()) return
        if (!manual && settingsRepository.syncFrequency.first() == SyncFrequency.NEVER) return

        val request = OneTimeWorkRequestBuilder<CourseSyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_DELAY_MINUTES,
                TimeUnit.MINUTES
            )
            .build()
        // KEEP: repeated calls (activity recreation, re-login) never restart a running sync.
        workManager.enqueueUniqueWork(
            UNIQUE_ONE_SHOT_WORK,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    override fun cancelAll() {
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
        workManager.cancelUniqueWork(UNIQUE_ONE_SHOT_WORK)
    }

    /**
     * Reads the persisted session, not the in-memory cache: right after a cold start the cache is
     * still empty, and treating that as "logged out" used to cancel the periodic sync.
     */
    private suspend fun hasSession(): Boolean = !tokenProvider.getToken().isNullOrBlank()

    private suspend fun networkConstraints(): Constraints {
        val wifiOnly = settingsRepository.wifiOnlySync.first()
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        return Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .setRequiresBatteryNotLow(true)
            .build()
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK = "CourseSyncWorker"
        const val UNIQUE_ONE_SHOT_WORK = "CourseSyncWorkerImmediate"
        private const val FIFTEEN_MINUTES_INTERVAL = 15L
        private const val TWELVE_HOURS_INTERVAL = 12L
        private const val BACKOFF_DELAY_MINUTES = 10L
    }
}
