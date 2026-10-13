package it.polimi.webeep.feature.auth.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.LoginResult
import it.polimi.webeep.feature.auth.domain.LoginUseCase
import it.polimi.webeep.feature.auth.domain.SsoCallbackRelay
import it.polimi.webeep.feature.sync.domain.SyncScheduling
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(@StringRes val messageRes: Int) : LoginUiState
}

/**
 * Login flow and SSO callback. Same MVVM shape as the other screens:
 * public methods mutate [uiState], no intent dispatcher.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduling,
    private val ssoCallbackRelay: SsoCallbackRelay
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            ssoCallbackRelay.tokens.collect { token -> handleSsoCallback(token) }
        }
        viewModelScope.launch {
            if (authRepository.isLoggedIn()) {
                _uiState.update { LoginUiState.Success }
            }
        }
    }

    fun generateSsoUrl(): String = loginUseCase.generateSsoLaunchUrl()

    fun resetState() {
        _uiState.update { LoginUiState.Idle }
    }

    fun clearError() {
        _uiState.update { state ->
            if (state is LoginUiState.Error) LoginUiState.Idle else state
        }
    }

    fun handleSsoCallback(encodedToken: String) {
        if (encodedToken.isBlank() || encodedToken == "{token}") {
            return
        }
        _uiState.update { LoginUiState.Loading }
        viewModelScope.launch {
            when (val result = loginUseCase.execute(encodedToken)) {
                is LoginResult.Success -> {
                    syncScheduler.applyCurrentSettings()
                    syncScheduler.enqueueImmediate()
                    _uiState.update { LoginUiState.Success }
                }
                is LoginResult.Failure -> {
                    _uiState.update { LoginUiState.Error(result.error.toMessageRes()) }
                }
            }
        }
    }
}
