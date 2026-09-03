package it.polimi.webeep.feature.dashboard.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.ui.components.CourseUiModel
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.feature.auth.domain.AuthRepository
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.GetCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.SyncCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.core.util.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val courses: List<CourseUiModel>,
        val userName: String,
        val userAvatar: String,
        val isRefreshing: Boolean = false
    ) : DashboardUiState
    data class Error(val message: String) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getCoursesUseCase: GetCoursesUseCase,
    private val syncCoursesUseCase: SyncCoursesUseCase,
    private val dashboardRepository: DashboardRepository,
    private val authRepository: AuthRepository,
    private val networkMonitor: NetworkMonitor
) : ViewModel() {

    private var coursesJob: Job? = null

    val isOnline: StateFlow<Boolean> = networkMonitor.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = true
        )

    private data class VmState(
        val userName: String = "Studente",
        val userAvatar: String = "",
        val courses: List<CourseUiModel>? = null,
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        val error: String? = null
    ) {
        fun toUiState(): DashboardUiState {
            if (isLoading) return DashboardUiState.Loading
            if (error != null) return DashboardUiState.Error(error)
            return DashboardUiState.Success(
                courses = courses ?: emptyList(),
                userName = userName,
                userAvatar = userAvatar,
                isRefreshing = isRefreshing
            )
        }
    }

    private val _vmState = MutableStateFlow(VmState())
    val uiState: StateFlow<DashboardUiState> = _vmState
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardUiState.Loading
        )

    init {
        viewModelScope.launch {
            loadUserInfo()
        }
        loadCourses()
    }

    private suspend fun loadUserInfo() {
        dashboardRepository.getUserInfo().fold(
            onSuccess = { (name, avatar) ->
                _vmState.update { it.copy(userName = name, userAvatar = avatar) }
            },
            onFailure = {
            }
        )
    }

    private suspend fun loadCoursesInternal() {
        if (_vmState.value.courses == null) {
            _vmState.update { it.copy(isLoading = true, error = null) }
        }
        getCoursesUseCase().collectLatest { resource ->
            when (resource) {
                is Resource.Loading -> {
                    if (_vmState.value.courses == null) {
                        _vmState.update { it.copy(isLoading = true, error = null) }
                    }
                }
                is Resource.Success -> {
                    _vmState.update {
                        it.copy(
                            isLoading = false,
                            courses = resource.data.map { course -> course.toUiModel() },
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    _vmState.update { it.copy(isLoading = false, error = resource.message) }
                }
            }
        }
    }

    fun loadCourses() {
        coursesJob?.cancel()
        coursesJob = viewModelScope.launch {
            loadCoursesInternal()
        }
    }

    fun refresh() {
        _vmState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            syncCoursesUseCase().fold(
                onSuccess = { courses ->
                    _vmState.update {
                        it.copy(
                            isRefreshing = false,
                            courses = courses.map { course -> course.toUiModel() },
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { throwable ->
                    _vmState.update {
                        val newError = if (it.courses == null) {
                            throwable.localizedMessage ?: "Impossibile aggiornare i corsi"
                        } else {
                            null // keep existing data
                        }
                        it.copy(isRefreshing = false, error = newError, isLoading = false)
                    }
                }
            )
        }
    }

    fun logout(onComplete: () -> Unit = {}) {
        viewModelScope.launch {
            authRepository.logout()
            onComplete()
        }
    }

    private fun Course.toUiModel() = CourseUiModel(
        id = id,
        shortname = shortname,
        fullname = fullname,
        courseImage = courseImage,
        progress = progress,
        isFavourite = isFavourite,
        category = category,
        endDate = endDate
    )
}
