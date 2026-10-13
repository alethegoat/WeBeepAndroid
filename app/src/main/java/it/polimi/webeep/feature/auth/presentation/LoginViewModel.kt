package it.polimi.webeep.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.R
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.LoginError
import it.polimi.webeep.feature.auth.domain.LoginResult
import it.polimi.webeep.feature.auth.domain.LoginUseCase
import it.polimi.webeep.feature.auth.domain.SsoCallbackRelay
import it.polimi.webeep.feature.sync.SyncScheduler
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
    data class Error(val message: String) : LoginUiState
}

/**
 * Login flow and SSO callback. Same MVVM shape as the other screens:
 * public methods mutate [uiState], no intent dispatcher.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository,
    private val syncScheduler: SyncScheduler,
    private val ssoCallbackRelay: SsoCallbackRelay,
    private val application: android.app.Application
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
                    _uiState.update { LoginUiState.Error(result.error.toMessage()) }
                }
            }
        }
    }

    private fun LoginError.toMessage(): String = when (this) {
        LoginError.InvalidTokenFormat -> application.getString(R.string.error_login_invalid_format)
        LoginError.SessionExpired -> application.getString(R.string.error_login_session_expired)
        LoginError.InvalidPassport -> application.getString(R.string.error_login_invalid_passport)
        LoginError.InvalidAccessToken -> application.getString(R.string.error_login_invalid_token)
        LoginError.TokenProcessingFailed -> application.getString(R.string.error_login_process_failed)
        is LoginError.ValidationFailed -> application.getString(R.string.error_login_validation_failed)
    }
}
