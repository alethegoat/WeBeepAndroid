package it.polimi.webeep.feature.dashboard.presentation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@Composable
fun DashboardRoute(
    onCourseClick: (courseId: Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    DashboardScreen(
        // The text field reads the synchronous query; everything else comes from the flow.
        state = state.copy(searchQuery = viewModel.searchQuery),
        onAction = viewModel::onAction,
        onCourseClick = onCourseClick,
        onSettingsClick = onSettingsClick,
        modifier = modifier
    )
}
