package it.polimi.webeep.feature.dashboard.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import it.polimi.webeep.core.ui.util.bouncyClickable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import it.polimi.webeep.core.ui.util.ScrollBoundaryHaptics
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.CourseCard
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.components.ShimmerCourseList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel,
    onCourseClick: (courseId: Int) -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val isOnline by viewModel.isOnline.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var courseFilter by remember { mutableStateOf(CourseFilter.Active) }

    val haptic = LocalHapticFeedback.current
    val lazyGridState = rememberLazyGridState()
    val context = LocalContext.current

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    it.polimi.webeep.core.ui.components.AuroraBackground {
        Scaffold(
            modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            containerColor = Color.Transparent,
            topBar = {
                val backgroundColor = MaterialTheme.colorScheme.background
                TopAppBar(
                    modifier = Modifier.drawBehind {
                        val fraction = scrollBehavior.state.overlappedFraction.coerceIn(0f, 1f)
                        if (fraction > 0f) {
                            // To make it melt beautifully into the Aurora, we extend the bottom transparent part
                            drawRect(
                                brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(
                                        backgroundColor.copy(alpha = 0.95f * fraction),
                                        backgroundColor.copy(alpha = 0.5f * fraction),
                                        androidx.compose.ui.graphics.Color.Transparent
                                    )
                                )
                            )
                        }
                    },
                    title = {
                        AnimatedVisibility(
                            visible = scrollBehavior.state.overlappedFraction > 0.5f,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = onSettingsClick,
                            colors = IconButtonDefaults.iconButtonColors(
                                contentColor = MaterialTheme.colorScheme.onSurface
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = stringResource(R.string.cd_settings)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            val isCacheAvailable = (uiState as? DashboardUiState.Success)?.courses?.isNotEmpty() == true
            val avatarUrl = (uiState as? DashboardUiState.Success)?.userAvatar ?: ""
            val userName = (uiState as? DashboardUiState.Success)?.userName ?: stringResource(R.string.student_placeholder)

            Box(modifier = Modifier.fillMaxSize()) {
                when (val state = uiState) {
                    is DashboardUiState.Loading -> {
                        Column(modifier = Modifier.padding(top = innerPadding.calculateTopPadding(), start = 16.dp, end = 16.dp)) {
                            ShimmerCourseList()
                        }
                    }
                    is DashboardUiState.Success -> {
                        val filteredCourses = remember(state.courses, searchQuery, courseFilter) {
                            state.courses.filter { course ->
                                val matchesSearch = course.fullname.contains(searchQuery, ignoreCase = true) ||
                                        course.shortname.contains(searchQuery, ignoreCase = true)
                                
                                val now = System.currentTimeMillis() / 1000
                                val matchesFilter = when (courseFilter) {
                                    CourseFilter.All -> true
                                    CourseFilter.Active -> course.endDate == 0L || course.endDate >= now
                                    CourseFilter.Past -> course.endDate != 0L && course.endDate < now
                                }
                                matchesSearch && matchesFilter
                            }
                        }

                        PullToRefreshBox(
                            isRefreshing = state.isRefreshing,
                            onRefresh = {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                viewModel.refresh()
                            },
                            modifier = Modifier.fillMaxSize()
                        ) {
                            ScrollBoundaryHaptics(state = lazyGridState)

                            LazyVerticalGrid(
                                state = lazyGridState,
                                columns = GridCells.Adaptive(minSize = 300.dp),
                                contentPadding = PaddingValues(
                                    top = innerPadding.calculateTopPadding() + 8.dp,
                                    bottom = 16.dp,
                                    start = 16.dp,
                                    end = 16.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                item(span = { GridItemSpan(maxLineSpan) }) {
                                    Column {
                                        // welcome back header
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            AsyncImage(
                                                model = avatarUrl.ifEmpty { null },
                                                contentDescription = stringResource(R.string.cd_user_avatar),
                                                modifier = Modifier
                                                    .size(56.dp)
                                                    .clip(CircleShape)
                                                    .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                                                contentScale = ContentScale.Crop
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Column {
                                                Text(
                                                    text = stringResource(R.string.welcome_back),
                                                    style = MaterialTheme.typography.titleMedium,
                                                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                                                )
                                                Text(
                                                    text = userName,
                                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onBackground
                                                )
                                            }
                                        }

                                        // offline banner
                                        AnimatedVisibility(
                                            visible = !isOnline,
                                            enter = expandVertically() + fadeIn(),
                                            exit = shrinkVertically() + fadeOut()
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(vertical = 8.dp)
                                                    .clip(RoundedCornerShape(16.dp))
                                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.9f))
                                                    .padding(vertical = 12.dp, horizontal = 16.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.Center
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.CloudOff,
                                                        contentDescription = null,
                                                        tint = MaterialTheme.colorScheme.onErrorContainer,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Text(
                                                        text = if (isCacheAvailable) stringResource(R.string.offline_cache_message) else stringResource(R.string.offline_no_connection_message),
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = MaterialTheme.colorScheme.onErrorContainer,
                                                        fontWeight = FontWeight.Medium
                                                    )
                                                }
                                            }
                                        }

                                        // search section
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp)
                                        ) {
                                            TextField(
                                                value = searchQuery,
                                                onValueChange = { searchQuery = it },
                                                placeholder = { 
                                                    Text(
                                                        text = stringResource(R.string.search_courses_placeholder),
                                                        style = MaterialTheme.typography.bodyMedium,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                                    ) 
                                                },
                                                leadingIcon = {
                                                    Icon(
                                                        imageVector = Icons.Default.Search,
                                                        contentDescription = stringResource(R.string.cd_search),
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                        modifier = Modifier.size(20.dp)
                                                    )
                                                },
                                                trailingIcon = {
                                                    if (searchQuery.isNotEmpty()) {
                                                        IconButton(onClick = { searchQuery = "" }) {
                                                            Icon(
                                                                imageVector = Icons.Default.Close,
                                                                contentDescription = stringResource(R.string.clear_search),
                                                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                        }
                                                    }
                                                },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(48.dp),
                                                singleLine = true,
                                                shape = CircleShape,
                                                colors = TextFieldDefaults.colors(
                                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                                    focusedIndicatorColor = Color.Transparent,
                                                    unfocusedIndicatorColor = Color.Transparent,
                                                    disabledIndicatorColor = Color.Transparent
                                                ),
                                                textStyle = MaterialTheme.typography.bodyMedium
                                            )
                                        }

                                        // course filter segmented control
                                        BoxWithConstraints(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                                                .padding(4.dp)
                                        ) {
                                            val filters = listOf(
                                                CourseFilter.Active to stringResource(R.string.filter_active),
                                                CourseFilter.Past to stringResource(R.string.filter_past),
                                                CourseFilter.All to stringResource(R.string.filter_all)
                                            )
                                            val pillWidth = maxWidth / filters.size
                                            
                                            val selectedIndex = filters.indexOfFirst { it.first == courseFilter }
                                            val pillOffset by animateDpAsState(
                                                targetValue = pillWidth * selectedIndex,
                                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                                                label = "pillOffset"
                                            )

                                            // pill background
                                            Box(
                                                modifier = Modifier
                                                    .offset { androidx.compose.ui.unit.IntOffset(pillOffset.roundToPx(), 0) }
                                                    .width(pillWidth)
                                                    .height(36.dp) // forced height
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(MaterialTheme.colorScheme.primary)
                                            )

                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                filters.forEach { (filter, label) ->
                                                    val isSelected = courseFilter == filter
                                                    val contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
                                                    
                                                    Box(
                                                        modifier = Modifier
                                                            .width(pillWidth)
                                                            .height(36.dp)
                                                            .bouncyClickable { courseFilter = filter },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = label,
                                                            style = MaterialTheme.typography.bodyMedium.copy(
                                                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                                            ),
                                                            color = contentColor
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                        
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }

                                if (filteredCourses.isEmpty()) {
                                    item(span = { GridItemSpan(maxLineSpan) }) {
                                        EmptyStateView(
                                            icon = Icons.Default.Book,
                                            title = stringResource(R.string.no_courses_found),
                                            subtitle = if (searchQuery.isNotEmpty() || courseFilter != CourseFilter.Active) {
                                                stringResource(R.string.try_changing_filters)
                                            } else {
                                                stringResource(R.string.no_courses_enrolled)
                                            },
                                            actionLabel = if (searchQuery.isNotEmpty() || courseFilter != CourseFilter.Active) stringResource(R.string.reset_filters) else stringResource(R.string.refresh),
                                            onAction = {
                                                if (searchQuery.isNotEmpty() || courseFilter != CourseFilter.Active) {
                                                    searchQuery = ""
                                                    courseFilter = CourseFilter.Active
                                                } else {
                                                    viewModel.refresh()
                                                }
                                            }
                                        )
                                    }
                                } else {
                                    items(
                                        items = filteredCourses,
                                        key = { it.id },
                                        contentType = { "course_card" }
                                    ) { course ->
                                        CourseCard(
                                            course = course,
                                            onClick = { onCourseClick(course.id) },
                                            modifier = Modifier.animateItem()
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is DashboardUiState.Error -> {
                        EmptyStateView(
                            icon = Icons.Default.Warning,
                            title = stringResource(R.string.dashboard_error_title),
                            subtitle = state.message,
                            actionLabel = stringResource(R.string.retry),
                            onAction = { viewModel.loadCourses() },
                            modifier = Modifier.padding(innerPadding)
                        )
                    }
                }
            }
        }
    }
}

enum class CourseFilter {
    Active, Past, All
}
