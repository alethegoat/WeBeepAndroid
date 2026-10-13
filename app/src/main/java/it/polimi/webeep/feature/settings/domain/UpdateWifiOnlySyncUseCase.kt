package it.polimi.webeep.feature.settings.domain

import it.polimi.webeep.feature.sync.domain.SyncScheduling
import javax.inject.Inject

/** Persists the flag first: the scheduler builds its network constraints from it. */
class UpdateWifiOnlySyncUseCase @Inject constructor(private val settings: SettingsRepository, private val scheduling: SyncScheduling) {
    /** Saves the flag and reschedules background sync with the new network constraint. */
    suspend operator fun invoke(enabled: Boolean) {
        settings.setWifiOnlySync(enabled)
        scheduling.applyCurrentSettings()
    }
}
