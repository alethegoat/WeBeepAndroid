package it.polimi.webeep.feature.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import it.polimi.webeep.core.datastore.SettingsManager
import it.polimi.webeep.core.datastore.SyncFrequency
import it.polimi.webeep.core.datastore.TokenProvider
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SyncScheduler @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsManager: SettingsManager,
    private val tokenProvider: TokenProvider
) {
    private val workManager: WorkManager
        get() = WorkManager.getInstance(context)

    suspend fun applyCurrentSettings(replaceExisting: Boolean = true) {
        if (tokenProvider.getTokenSync().isNullOrBlank()) {
            cancelAll()
            return
        }

        val frequency = settingsManager.syncFrequency.first()
        if (frequency == SyncFrequency.NEVER) {
            workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
            return
        }

        val (interval, unit) = when (frequency) {
            SyncFrequency.FIFTEEN_MINUTES -> 15L to TimeUnit.MINUTES
            SyncFrequency.ONE_HOUR -> 1L to TimeUnit.HOURS
            SyncFrequency.TWELVE_HOURS -> 12L to TimeUnit.HOURS
            SyncFrequency.DAILY -> 1L to TimeUnit.DAYS
            SyncFrequency.NEVER -> return
        }

        val request = PeriodicWorkRequestBuilder<CourseSyncWorker>(interval, unit)
            .setConstraints(networkConstraints())
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10L,
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

    suspend fun enqueueImmediate() {
        if (tokenProvider.getTokenSync().isNullOrBlank()) return
        if (settingsManager.syncFrequency.first() == SyncFrequency.NEVER) return

        val request = OneTimeWorkRequestBuilder<CourseSyncWorker>()
            .setConstraints(networkConstraints())
            .setBackoffCriteria(
                androidx.work.BackoffPolicy.EXPONENTIAL,
                10L,
                TimeUnit.MINUTES
            )
            .build()
        workManager.enqueueUniqueWork(
            UNIQUE_ONE_SHOT_WORK,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun cancelAll() {
        workManager.cancelUniqueWork(UNIQUE_PERIODIC_WORK)
        workManager.cancelUniqueWork(UNIQUE_ONE_SHOT_WORK)
    }

    private suspend fun networkConstraints(): Constraints {
        val wifiOnly = settingsManager.wifiOnlySync.first()
        val networkType = if (wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED
        return Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()
    }

    companion object {
        const val UNIQUE_PERIODIC_WORK = "CourseSyncWorker"
        const val UNIQUE_ONE_SHOT_WORK = "CourseSyncWorkerImmediate"
        const val UNIQUE_WORK_NAME = UNIQUE_PERIODIC_WORK
    }
}
