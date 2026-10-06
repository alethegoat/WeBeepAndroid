package it.polimi.webeep.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.datastore.SettingsManager
import it.polimi.webeep.core.datastore.SyncFrequency
import it.polimi.webeep.core.datastore.ThemePreference
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.sync.SyncScheduler
import it.polimi.webeep.feature.sync.SyncStateStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM_DEFAULT,
    val dynamicColorPreference: Boolean = false,
    val wifiOnlySync: Boolean = false,
    val syncFrequency: SyncFrequency = SettingsManager.DEFAULT_SYNC_FREQUENCY
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsManager: SettingsManager,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    syncStateStore: SyncStateStore
) : ViewModel() {

    /** When the background sync last checked the courses for new files (epoch millis). */
    val lastCheckAt: StateFlow<Long?> = syncStateStore.lastCheckAt.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    /** Last-check time seen when "check now" was tapped; null when no manual check is running. */
    private data class CheckRequest(val previousCheckAt: Long?)

    private val checkRequest = MutableStateFlow<CheckRequest?>(null)

    /** True from "check now" until the background sync records a newer check. */
    val checkInProgress: StateFlow<Boolean> = combine(lastCheckAt, checkRequest) { last, request ->
        request != null && last == request.previousCheckAt
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

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
            syncScheduler.applyCurrentSettings()
        }
    }

    fun setSyncFrequency(frequency: SyncFrequency) {
        viewModelScope.launch {
            settingsManager.setSyncFrequency(frequency)
            syncScheduler.applyCurrentSettings()
        }
    }

    fun checkNow() {
        checkRequest.value = CheckRequest(previousCheckAt = lastCheckAt.value)
        viewModelScope.launch {
            syncScheduler.enqueueImmediate(manual = true)
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
