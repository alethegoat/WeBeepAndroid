package it.polimi.webeep.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.ui.util.STOP_TIMEOUT_MILLIS
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.settings.domain.CheckForUpdatesNowUseCase
import it.polimi.webeep.feature.settings.domain.SettingsRepository
import it.polimi.webeep.feature.settings.domain.UpdateSyncFrequencyUseCase
import it.polimi.webeep.feature.settings.domain.UpdateWifiOnlySyncUseCase
import it.polimi.webeep.feature.settings.domain.model.SyncFrequency
import it.polimi.webeep.feature.settings.domain.model.ThemePreference
import it.polimi.webeep.feature.sync.domain.SyncStateRepository
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val themePreference: ThemePreference = ThemePreference.SYSTEM_DEFAULT,
    val dynamicColorPreference: Boolean = false,
    val wifiOnlySync: Boolean = false,
    val syncFrequency: SyncFrequency = SyncFrequency.DEFAULT
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val updateWifiOnlySync: UpdateWifiOnlySyncUseCase,
    private val updateSyncFrequency: UpdateSyncFrequencyUseCase,
    private val checkForUpdatesNow: CheckForUpdatesNowUseCase,
    syncStateRepository: SyncStateRepository
) : ViewModel() {

    /** When the background sync last checked the courses for new files (epoch millis). */
    val lastCheckAt: StateFlow<Long?> = syncStateRepository.lastCheckAt.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
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
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = false
    )

    val uiState: StateFlow<SettingsUiState> = combine(
        settingsRepository.themePreference,
        settingsRepository.dynamicColorPreference,
        settingsRepository.wifiOnlySync,
        settingsRepository.syncFrequency
    ) { theme, dynamicColor, wifiOnly, syncFreq ->
        SettingsUiState(
            themePreference = theme,
            dynamicColorPreference = dynamicColor,
            wifiOnlySync = wifiOnly,
            syncFrequency = syncFreq
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState()
    )

    fun setThemePreference(theme: ThemePreference) {
        viewModelScope.launch {
            settingsRepository.setThemePreference(theme)
        }
    }

    fun setDynamicColorPreference(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicColorPreference(enabled)
        }
    }

    fun setWifiOnlySync(enabled: Boolean) {
        viewModelScope.launch {
            updateWifiOnlySync(enabled)
        }
    }

    fun setSyncFrequency(frequency: SyncFrequency) {
        viewModelScope.launch {
            updateSyncFrequency(frequency)
        }
    }

    fun checkNow() {
        checkRequest.value = CheckRequest(previousCheckAt = lastCheckAt.value)
        viewModelScope.launch {
            checkForUpdatesNow()
        }
    }

    fun logout(onComplete: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }
}
