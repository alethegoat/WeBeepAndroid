package it.polimi.webeep.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.datastore.SettingsManager
import it.polimi.webeep.core.datastore.SyncFrequency
import it.polimi.webeep.core.datastore.ThemePreference
import it.polimi.webeep.core.datastore.TokenManager
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM_DEFAULT,
    val dynamicColorPreference: Boolean = true,
    val wifiOnlySync: Boolean = true,
    val syncFrequency: SyncFrequency = SyncFrequency.FIFTEEN_MINUTES
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val tokenManager: TokenManager
) : ViewModel() {

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsManager.themePreference,
        settingsManager.dynamicColorPreference,
        settingsManager.wifiOnlySync,
        settingsManager.syncFrequency
    ) { theme, dynamicColor, wifiOnly, syncFreq ->
        SettingsUiState(
            themePreference = theme,
            dynamicColorPreference = dynamicColor,
            wifiOnlySync = wifiOnly,
            syncFrequency = syncFreq
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SettingsUiState()
    )

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            settingsManager.setThemePreference(theme)
        }
    }

    fun setDynamicColorPreference(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setDynamicColorPreference(enabled)
        }
    }

    fun setWifiOnlySync(enabled: Boolean) {
        viewModelScope.launch {
            settingsManager.setWifiOnlySync(enabled)
        }
    }

    fun setSyncFrequency(frequency: SyncFrequency) {
        viewModelScope.launch {
            settingsManager.setSyncFrequency(frequency)
            // notice: updating workmanager constraints/interval is not implemented yet.
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            tokenManager.clearAll()
            onComplete()
        }
    }
}
