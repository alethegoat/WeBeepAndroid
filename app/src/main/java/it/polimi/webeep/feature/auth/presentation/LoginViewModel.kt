package it.polimi.webeep.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.LoginUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

sealed interface LoginUiState {
    data object Idle : LoginUiState
    data object Loading : LoginUiState
    data object Success : LoginUiState
    data class Error(val message: String) : LoginUiState
}

sealed interface LoginIntent {
    data object CheckExistingSession : LoginIntent
    data object ResetState : LoginIntent
    data object ClearError : LoginIntent
    data class HandleSsoCallback(val encodedToken: String) : LoginIntent
}

/**
 * viewmodel to handle the login flow and sso authentication.
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
        onIntent(LoginIntent.CheckExistingSession)
    }

    fun onIntent(intent: LoginIntent) {
        when (intent) {
            is LoginIntent.CheckExistingSession -> checkExistingSession()
            is LoginIntent.ResetState -> _uiState.update { LoginUiState.Idle }
            is LoginIntent.ClearError -> {
                _uiState.update { state ->
                    if (state is LoginUiState.Error) LoginUiState.Idle else state
                }
            }
            is LoginIntent.HandleSsoCallback -> handleSsoCallbackInternal(intent.encodedToken)
        }
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            if (authRepository.isLoggedIn()) {
                _uiState.update { LoginUiState.Success }
            }
        }
    }

    /**
     * generates the url to start the sso login via browser or custom tab.
     */
    fun generateSsoUrl(): String {
        val passport = UUID.randomUUID().toString()
        // official url of webeep sso / launch.php for mobile app
        return "https://webeep.polimi.it/admin/tool/mobile/launch.php" +
                "?service=moodle_mobile_app" +
                "&passport=$passport" +
                "&urlscheme=webeepandroid"
    }

    // keeping for temporary backward compatibility with ui until it uses intents
    fun resetState() = onIntent(LoginIntent.ResetState)
    fun clearError() = onIntent(LoginIntent.ClearError)
    fun handleSsoCallback(encodedToken: String) = onIntent(LoginIntent.HandleSsoCallback(encodedToken))

    private fun handleSsoCallbackInternal(encodedToken: String) {
        if (encodedToken.isBlank() || encodedToken == "{token}") {
            return
        }
        _uiState.update { LoginUiState.Loading }
        viewModelScope.launch {
            when (val resource = loginUseCase.execute(encodedToken)) {
                is Resource.Success -> {
                    _uiState.update { LoginUiState.Success }
                }
                is Resource.Error -> {
                    _uiState.update { LoginUiState.Error(resource.message) }
                }
                is Resource.Loading -> {
                    _uiState.update { LoginUiState.Loading }
                }
            }
        }
    }


}
