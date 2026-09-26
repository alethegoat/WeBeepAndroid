package it.polimi.webeep.feature.dashboard.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.CourseCard
import it.polimi.webeep.core.ui.components.CourseUiModel
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.components.ShimmerCourseList
import it.polimi.webeep.core.ui.components.WeBeepSearchField
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.bouncyClickable

/** Minimum card width: one column on phones, more on tablets / landscape. */
private val CourseGridMinCellWidth = 320.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCourseClick: (courseId: Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isOnline by viewModel.isOnline.collectAsStateWithLifecycle()

    val lazyGridState = rememberLazyGridState()
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(lazyGridState.isScrollInProgress) {
        if (lazyGridState.isScrollInProgress) keyboard?.hide()
    }

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())
    val success = uiState as? DashboardUiState.Success

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = Color.Transparent,
        topBar = {
            DashboardTopBar(
                userName = success?.userName,
                avatarUrl = success?.userAvatar.orEmpty(),
                scrollBehavior = scrollBehavior,
                onSettingsClick = onSettingsClick
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is DashboardUiState.Loading -> {
                    ShimmerCourseList(
                        modifier = Modifier.padding(top = innerPadding.calculateTopPadding())
                    )
                }
                is DashboardUiState.Success -> {
                    DashboardCourseGrid(
                        state = state,
                        isOnline = isOnline,
                        gridState = lazyGridState,
                        contentPadding = innerPadding,
                        onRefresh = viewModel::refresh,
                        onCourseClick = onCourseClick
                    )
                }
                is DashboardUiState.Error -> {
                    EmptyStateView(
                        icon = Icons.Default.Warning,
                        title = stringResource(R.string.dashboard_error_title),
                        subtitle = stringResource(state.messageRes),
                        actionLabel = stringResource(R.string.retry),
                        onAction = { viewModel.loadCourses() },
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Greeting lives in the app bar so there is no empty band above it; the bar
 * turns opaque once content scrolls underneath (no see-through collapsed bar).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardTopBar(
    userName: String?,
    avatarUrl: String,
    scrollBehavior: TopAppBarScrollBehavior,
    onSettingsClick: () -> Unit
) {
    TopAppBar(
        title = {
            if (userName == null) {
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
            } else {
                GreetingHeader(
                    userName = userName.takeIf { it.isNotBlank() }
                        ?: stringResource(R.string.student_placeholder),
                    avatarUrl = avatarUrl
                )
            }
        },
        actions = {
            IconButton(onClick = onSettingsClick) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = stringResource(R.string.cd_settings)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurfaceVariant
        ),
        scrollBehavior = scrollBehavior
    )
}

@Composable
private fun GreetingHeader(
    userName: String,
    avatarUrl: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(WeBeepSpacing.touchTarget)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = userName.trim().take(1).uppercase(),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            if (avatarUrl.isNotEmpty()) {
                AsyncImage(
                    model = avatarUrl,
                    contentDescription = stringResource(R.string.cd_user_avatar),
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(modifier = Modifier.width(WeBeepSpacing.sm))
        Column {
            Text(
                text = stringResource(R.string.welcome_back),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = userName,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardCourseGrid(
    state: DashboardUiState.Success,
    isOnline: Boolean,
    gridState: LazyGridState,
    contentPadding: PaddingValues,
    onRefresh: () -> Unit,
    onCourseClick: (courseId: Int) -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    var searchQuery by rememberSaveable { mutableStateOf("") }
    var courseFilter by rememberSaveable { mutableStateOf(CourseFilter.Active) }
    val filteredCourses = remember(state.courses, searchQuery, courseFilter) {
        filterCourses(
            courses = state.courses,
            query = searchQuery,
            filter = courseFilter,
            nowEpochSeconds = System.currentTimeMillis() / 1000
        )
    }
    val filtersActive = searchQuery.isNotEmpty() || courseFilter != CourseFilter.Active

    PullToRefreshBox(
        isRefreshing = state.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.fillMaxSize()
    ) {
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Adaptive(minSize = CourseGridMinCellWidth),
            contentPadding = PaddingValues(
                top = contentPadding.calculateTopPadding() + WeBeepSpacing.xs,
                bottom = contentPadding.calculateBottomPadding() + WeBeepSpacing.md,
                start = WeBeepSpacing.md,
                end = WeBeepSpacing.md
            ),
            horizontalArrangement = Arrangement.spacedBy(WeBeepSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(WeBeepSpacing.sm),
            modifier = Modifier.fillMaxSize()
        ) {
            item(span = { GridItemSpan(maxLineSpan) }, contentType = "header") {
                DashboardListHeader(
                    isOnline = isOnline,
                    isCacheAvailable = state.courses.isNotEmpty(),
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    courseFilter = courseFilter,
                    onFilterChange = { courseFilter = it }
                )
            }

            if (filteredCourses.isEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "empty") {
                    EmptyStateView(
                        icon = Icons.Default.Book,
                        title = stringResource(R.string.no_courses_found),
                        subtitle = stringResource(
                            if (filtersActive) R.string.try_changing_filters else R.string.no_courses_enrolled
                        ),
                        actionLabel = stringResource(
                            if (filtersActive) R.string.reset_filters else R.string.refresh
                        ),
                        onAction = {
                            if (filtersActive) {
                                searchQuery = ""
                                courseFilter = CourseFilter.Active
                            } else {
                                onRefresh()
                            }
                        }
                    )
                }
            } else {
                items(
                    items = filteredCourses,
                    key = CourseUiModel::id,
                    contentType = { "course_card" }
                ) { course ->
                    CourseCard(
                        course = course,
                        onClick = { onCourseClick(course.id) },
                        modifier = Modifier.animateItem(
                            fadeInSpec = WeBeepMotion.enter(reduceMotion = reduceMotion),
                            fadeOutSpec = WeBeepMotion.exit(reduceMotion = reduceMotion),
                            placementSpec = WeBeepMotion.softSpring(reduceMotion)
                        )
                    )
                }
            }
        }
    }
}

@Composable
private fun DashboardListHeader(
    isOnline: Boolean,
    isCacheAvailable: Boolean,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    courseFilter: CourseFilter,
    onFilterChange: (CourseFilter) -> Unit
) {
    Column {
        AnimatedVisibility(
            visible = !isOnline,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            OfflineBanner(isCacheAvailable = isCacheAvailable)
        }

        WeBeepSearchField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            placeholder = stringResource(R.string.search_courses_placeholder),
            modifier = Modifier.padding(vertical = WeBeepSpacing.xxs)
        )

        CourseFilterBar(
            selected = courseFilter,
            onSelect = onFilterChange,
            modifier = Modifier.padding(vertical = WeBeepSpacing.xs)
        )
    }
}

@Composable
private fun OfflineBanner(isCacheAvailable: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = WeBeepSpacing.xxs)
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(vertical = WeBeepSpacing.sm, horizontal = WeBeepSpacing.md)
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onErrorContainer,
            modifier = Modifier.size(16.dp)
        )
        Spacer(modifier = Modifier.width(WeBeepSpacing.xs))
        Text(
            text = stringResource(
                if (isCacheAvailable) R.string.offline_cache_message else R.string.offline_no_connection_message
            ),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun CourseFilterBar(
    selected: CourseFilter,
    onSelect: (CourseFilter) -> Unit,
    modifier: Modifier = Modifier
) {
    val reduceMotion = LocalReduceMotion.current
    val filters = listOf(
        CourseFilter.Active to stringResource(R.string.filter_active),
        CourseFilter.Past to stringResource(R.string.filter_past),
        CourseFilter.All to stringResource(R.string.filter_all)
    )
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(WeBeepSpacing.xxs)
    ) {
        val pillWidth = maxWidth / filters.size
        val selectedIndex = filters.indexOfFirst { it.first == selected }
        val pillOffset by animateDpAsState(
            targetValue = pillWidth * selectedIndex,
            animationSpec = WeBeepMotion.softSpring(reduceMotion),
            label = "pillOffset"
        )

        Box(
            modifier = Modifier
                .offset { IntOffset(pillOffset.roundToPx(), 0) }
                .width(pillWidth)
                .height(WeBeepSpacing.touchTarget)
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.primary)
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            filters.forEach { (filter, label) ->
                val isSelected = selected == filter
                val contentColor by animateColorAsState(
                    targetValue = if (isSelected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    animationSpec = WeBeepMotion.smooth(reduceMotion = reduceMotion),
                    label = "filterColor"
                )
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .height(WeBeepSpacing.touchTarget)
                        .semantics {
                            role = Role.Tab
                            this.selected = isSelected
                        }
                        .bouncyClickable(
                            role = Role.Tab,
                            onClickLabel = label
                        ) { onSelect(filter) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        ),
                        color = contentColor
                    )
                }
            }
        }
    }
}

enum class CourseFilter {
    Active, Past, All
}
