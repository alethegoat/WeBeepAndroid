package it.polimi.webeep.util.fakes

import it.polimi.webeep.feature.settings.domain.SettingsRepository
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSettingsRepository : SettingsRepository {
    override val themePreference = MutableStateFlow(ThemePreference.SYSTEM_DEFAULT)
    override val dynamicColorPreference = MutableStateFlow(false)
    override val wifiOnlySync = MutableStateFlow(false)
    override val syncFrequency = MutableStateFlow(SyncFrequency.DEFAULT)

    /** Order of the setters called, to assert persistence happens before rescheduling. */
    val events = mutableListOf<String>()

    override suspend fun setThemePreference(theme: ThemePreference) {
        themePreference.value = theme
    }

    override suspend fun setDynamicColorPreference(enabled: Boolean) {
        dynamicColorPreference.value = enabled
    }

    override suspend fun setWifiOnlySync(enabled: Boolean) {
        events += "wifiOnly=$enabled"
        wifiOnlySync.value = enabled
    }

    override suspend fun setSyncFrequency(frequency: SyncFrequency) {
        events += "frequency=$frequency"
        syncFrequency.value = frequency
    }
}
