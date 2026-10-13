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
import it.polimi.webeep.feature.sync.domain.SyncStateRepository
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * State and actions of the settings screen. "Check now" shows progress until the sync records a check newer than the one
 * seen when it was tapped; logout emits [SettingsEffect.LoggedOut] only after local data is wiped.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val authRepository: AuthRepository,
    private val updateWifiOnlySync: UpdateWifiOnlySyncUseCase,
    private val updateSyncFrequency: UpdateSyncFrequencyUseCase,
    private val checkForUpdatesNow: CheckForUpdatesNowUseCase,
    syncStateRepository: SyncStateRepository
) : ViewModel() {

    /** Last-check time seen when "check now" was tapped; null when no manual check is running. */
    private data class CheckRequest(val previousCheckAt: Long?)

    private val checkRequest = MutableStateFlow<CheckRequest?>(null)

    private val preferences = combine(
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
    }

    val uiState: StateFlow<SettingsUiState> = combine(
        preferences,
        syncStateRepository.lastCheckAt,
        checkRequest
    ) { prefs, lastCheckAt, request ->
        prefs.copy(
            lastCheckAt = lastCheckAt,
            // True from "check now" until the background sync records a newer check.
            checkInProgress = request != null && lastCheckAt == request.previousCheckAt
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
        initialValue = SettingsUiState()
    )

    private val _effects = Channel<SettingsEffect>(Channel.BUFFERED)
    val effects: Flow<SettingsEffect> = _effects.receiveAsFlow()

    fun onAction(action: SettingsAction) {
        when (action) {
            is SettingsAction.ThemeChange -> viewModelScope.launch {
                settingsRepository.setThemePreference(action.theme)
            }
            is SettingsAction.DynamicColorChange -> viewModelScope.launch {
                settingsRepository.setDynamicColorPreference(action.enabled)
            }
            is SettingsAction.WifiOnlyChange -> viewModelScope.launch { updateWifiOnlySync(action.enabled) }
            is SettingsAction.FrequencyChange -> viewModelScope.launch { updateSyncFrequency(action.frequency) }
            SettingsAction.CheckNow -> checkNow()
            SettingsAction.Logout -> logout()
        }
    }

    private fun checkNow() {
        checkRequest.value = CheckRequest(previousCheckAt = uiState.value.lastCheckAt)
        viewModelScope.launch {
            checkForUpdatesNow()
        }
    }

    private fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _effects.send(SettingsEffect.LoggedOut)
        }
    }
}
