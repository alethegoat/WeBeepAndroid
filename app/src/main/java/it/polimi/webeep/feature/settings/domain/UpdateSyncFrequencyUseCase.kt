package it.polimi.webeep.feature.settings.domain

import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.sync.domain.SyncScheduling
import javax.inject.Inject

/** Persists the frequency first: the scheduler reads it back from the repository. */
class UpdateSyncFrequencyUseCase @Inject constructor(private val settings: SettingsRepository, private val scheduling: SyncScheduling) {
    /** Saves [frequency] and reschedules (or cancels, for [SyncFrequency.NEVER]) background sync. */
    suspend operator fun invoke(frequency: SyncFrequency) {
        settings.setSyncFrequency(frequency)
        scheduling.applyCurrentSettings()
    }
}
