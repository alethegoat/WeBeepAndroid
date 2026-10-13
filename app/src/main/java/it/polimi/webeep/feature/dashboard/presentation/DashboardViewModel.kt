package it.polimi.webeep.feature.dashboard.presentation

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.ui.components.CourseUiModel
import it.polimi.webeep.core.ui.util.STOP_TIMEOUT_MILLIS
import it.polimi.webeep.core.util.ConnectivityObserver
import it.polimi.webeep.core.util.Resource
import it.polimi.webeep.core.util.toMessageRes
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.GetCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.SyncCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.model.Course
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface DashboardUiState {
    data object Loading : DashboardUiState
    data class Success(
        val courses: List<CourseUiModel>,
        /** Blank until the profile is loaded; the screen shows a localized placeholder. */
        val userName: String,
        val userAvatar: String,
        val isRefreshing: Boolean = false
    ) : DashboardUiState
    data class Error(@StringRes val messageRes: Int) : DashboardUiState
}

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getCoursesUseCase: GetCoursesUseCase,
    private val syncCoursesUseCase: SyncCoursesUseCase,
    private val dashboardRepository: DashboardRepository,
    connectivityObserver: ConnectivityObserver
) : ViewModel() {

    private var coursesJob: Job? = null

    val isOnline: StateFlow<Boolean> = connectivityObserver.isOnline
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = true
        )

    private data class VmState(
        val userName: String = "",
        val userAvatar: String = "",
        val courses: List<CourseUiModel>? = null,
        val isLoading: Boolean = true,
        val isRefreshing: Boolean = false,
        @StringRes val error: Int? = null
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

    private val vmState = MutableStateFlow(VmState())
    val uiState: StateFlow<DashboardUiState> = vmState
        .map { it.toUiState() }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
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
                vmState.update { it.copy(userName = name, userAvatar = avatar) }
            },
            onFailure = {
            }
        )
    }

    private suspend fun loadCoursesInternal() {
        if (vmState.value.courses == null) {
            vmState.update { it.copy(isLoading = true, error = null) }
        }
        getCoursesUseCase().collectLatest { resource ->
            when (resource) {
                is Resource.Success -> {
                    vmState.update {
                        it.copy(
                            isLoading = false,
                            courses = resource.data.map { course -> course.toUiModel() },
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    vmState.update { it.copy(isLoading = false, error = resource.error.toMessageRes()) }
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
        vmState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            when (val result = syncCoursesUseCase()) {
                is Resource.Success -> {
                    vmState.update {
                        it.copy(
                            isRefreshing = false,
                            courses = result.data.map { course -> course.toUiModel() },
                            isLoading = false,
                            error = null
                        )
                    }
                }
                is Resource.Error -> {
                    vmState.update {
                        // Keep existing data; surface the error only when there is nothing to show.
                        val newError = if (it.courses == null) result.error.toMessageRes() else null
                        it.copy(isRefreshing = false, error = newError, isLoading = false)
                    }
                }
            }
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
