package it.polimi.webeep.feature.auth.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.auth.domain.LoginUseCase
import it.polimi.webeep.feature.sync.SyncScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

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
    private val syncScheduler: SyncScheduler
) : ViewModel() {

    private val _uiState = MutableStateFlow<LoginUiState>(LoginUiState.Idle)
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    init {
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
            when (val resource = loginUseCase.execute(encodedToken)) {
                is Resource.Success -> {
                    syncScheduler.applyCurrentSettings()
                    syncScheduler.enqueueImmediate()
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
