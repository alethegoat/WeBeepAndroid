package it.polimi.webeep.feature.settings.domain

import it.polimi.webeep.feature.sync.domain.SyncScheduling
import javax.inject.Inject

/** Runs a sync even when background sync is set to never. */
class CheckForUpdatesNowUseCase @Inject constructor(private val scheduling: SyncScheduling) {
    suspend operator fun invoke() {
        scheduling.enqueueImmediate(manual = true)
    }
}
