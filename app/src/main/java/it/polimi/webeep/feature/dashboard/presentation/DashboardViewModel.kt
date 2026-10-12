package it.polimi.webeep.feature.dashboard.presentation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import it.polimi.webeep.core.common.ConnectivityObserver
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.onSuccess
import it.polimi.webeep.core.ui.util.toMessageRes
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.GetCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.SyncCoursesUseCase
import it.polimi.webeep.feature.dashboard.domain.model.Course
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_QUERY_KEY = "dashboard_search_query"
private const val FILTER_KEY = "dashboard_filter"

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getCoursesUseCase: GetCoursesUseCase,
    private val syncCoursesUseCase: SyncCoursesUseCase,
    private val dashboardRepository: DashboardRepository,
    connectivityObserver: ConnectivityObserver,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private var coursesJob: Job? = null

    /**
     * The text field's value. Compose state, not part of the flow round-trip, so typing stays
     * synchronous (no dropped characters or cursor jumps); [uiState] mirrors it for filtering.
     */
    var searchQuery by mutableStateOf(savedStateHandle[SEARCH_QUERY_KEY] ?: "")
        private set

    private val _uiState = MutableStateFlow(
        DashboardUiState(
            searchQuery = searchQuery,
            filter = savedStateHandle.get<String>(FILTER_KEY)
                ?.let { name -> CourseFilter.entries.firstOrNull { it.name == name } }
                ?: DEFAULT_COURSE_FILTER
        )
    )
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            connectivityObserver.isOnline.collect { online ->
                _uiState.update { it.copy(isOnline = online) }
            }
        }
        viewModelScope.launch {
            loadUserInfo()
        }
        loadCourses()
    }

    fun onAction(action: DashboardAction) {
        when (action) {
            DashboardAction.Refresh -> refresh()
            DashboardAction.Retry -> loadCourses()
            DashboardAction.ResetFilters -> applyFilters(query = "", filter = DEFAULT_COURSE_FILTER)
            is DashboardAction.SearchQueryChange -> applyFilters(query = action.query, filter = _uiState.value.filter)
            is DashboardAction.FilterChange -> applyFilters(query = searchQuery, filter = action.filter)
        }
    }

    private fun applyFilters(query: String, filter: CourseFilter) {
        searchQuery = query
        savedStateHandle[SEARCH_QUERY_KEY] = query
        savedStateHandle[FILTER_KEY] = filter.name
        _uiState.update { state ->
            val withFilters = state.copy(searchQuery = query, filter = filter)
            val content = state.content
            if (content is DashboardContent.Success) {
                withFilters.copy(content = content.copy(visibleCourses = withFilters.visible(content.courses)))
            } else {
                withFilters
            }
        }
    }

    private suspend fun loadUserInfo() {
        // A failure is deliberately silent: the header keeps its placeholder name and avatar,
        // and the courses load (and report their own errors) independently.
        dashboardRepository.getUserInfo().onSuccess { profile ->
            _uiState.update { it.copy(userName = profile.fullName, userAvatar = profile.avatarUrl.orEmpty()) }
        }
    }

    private suspend fun loadCoursesInternal() {
        if (_uiState.value.content !is DashboardContent.Success) {
            _uiState.update { it.copy(content = DashboardContent.Loading) }
        }
        getCoursesUseCase().collectLatest { resource ->
            when (resource) {
                is Resource.Success -> showCourses(resource.data)
                is Resource.Error -> _uiState.update {
                    it.copy(content = DashboardContent.Error(resource.error.toMessageRes()))
                }
            }
        }
    }

    private fun loadCourses() {
        coursesJob?.cancel()
        coursesJob = viewModelScope.launch {
            loadCoursesInternal()
        }
    }

    private fun refresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            when (val result = syncCoursesUseCase()) {
                is Resource.Success -> {
                    showCourses(result.data)
                    _uiState.update { it.copy(isRefreshing = false) }
                }
                is Resource.Error -> _uiState.update {
                    // Keep existing data; surface the error only when there is nothing to show.
                    val content = if (it.content is DashboardContent.Success) {
                        it.content
                    } else {
                        DashboardContent.Error(result.error.toMessageRes())
                    }
                    it.copy(isRefreshing = false, content = content)
                }
            }
        }
    }

    private fun showCourses(courses: List<Course>) {
        val models = courses.map { it.toUiModel() }
        _uiState.update {
            it.copy(content = DashboardContent.Success(courses = models, visibleCourses = it.visible(models)))
        }
    }

    private fun DashboardUiState.visible(courses: List<CourseUiModel>) = filterCourses(
        courses = courses,
        query = searchQuery,
        filter = filter,
        nowEpochSeconds = System.currentTimeMillis() / 1000
    )

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
