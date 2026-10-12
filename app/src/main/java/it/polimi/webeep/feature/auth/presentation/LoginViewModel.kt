package it.polimi.webeep.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.LoginResult
import it.polimi.webeep.feature.auth.domain.LoginUseCase
import it.polimi.webeep.feature.auth.domain.SsoCallbackRelay
import it.polimi.webeep.feature.sync.domain.SyncScheduling
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Login flow and SSO callback. The state only says whether a callback is being validated;
 * the outcome (logged in, failed) is a one-shot [LoginEffect].
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

    private val _effects = Channel<LoginEffect>(Channel.BUFFERED)
    val effects: Flow<LoginEffect> = _effects.receiveAsFlow()

    init {
        viewModelScope.launch {
            ssoCallbackRelay.tokens.collect { token -> handleSsoCallback(token) }
        }
        viewModelScope.launch {
            if (authRepository.isLoggedIn()) {
                _effects.send(LoginEffect.LoggedIn)
            }
        }
    }

    fun generateSsoUrl(): String = loginUseCase.generateSsoLaunchUrl()

    private fun handleSsoCallback(encodedToken: String) {
        if (encodedToken.isBlank() || encodedToken == "{token}") {
            return
        }
        _uiState.update { LoginUiState.Loading }
        viewModelScope.launch {
            when (val result = loginUseCase.execute(encodedToken)) {
                is LoginResult.Success -> {
                    syncScheduler.applyCurrentSettings()
                    syncScheduler.enqueueImmediate()
                    _uiState.update { LoginUiState.Idle }
                    _effects.send(LoginEffect.LoggedIn)
                }
                is LoginResult.Failure -> {
                    _uiState.update { LoginUiState.Idle }
                    _effects.send(LoginEffect.ShowError(result.error.toMessageRes()))
                }
            }
        }
    }
}
