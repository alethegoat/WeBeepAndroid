package it.polimi.webeep.feature.dashboard.presentation

/** User intents the dashboard sends to its ViewModel. */
sealed interface DashboardAction {
    /** Pull-to-refresh: sync from the network now. */
    data object Refresh : DashboardAction

    /** Reload after an error. */
    data object Retry : DashboardAction
    data object ResetFilters : DashboardAction
    data class SearchQueryChange(val query: String) : DashboardAction
    data class FilterChange(val filter: CourseFilter) : DashboardAction
}
