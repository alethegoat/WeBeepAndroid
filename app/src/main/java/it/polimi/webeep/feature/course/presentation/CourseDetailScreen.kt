package it.polimi.webeep.feature.course.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.components.ShimmerCourseList
import it.polimi.webeep.core.ui.components.WeBeepSearchField
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.courseSharedBounds
import it.polimi.webeep.core.util.CourseTitle
import it.polimi.webeep.core.util.CourseTitleParser

private val MaxContentWidth = 840.dp
private val EmptySearchHeight = 320.dp

@Composable
fun CourseDetailScreen(
    viewModel: CourseDetailViewModel,
    onPreviewFile: (FileUiModel) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lazyListState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    var searchQuery by rememberSaveable { mutableStateOf("") }

    LaunchedEffect(lazyListState.isScrollInProgress) {
        if (lazyListState.isScrollInProgress) keyboard?.hide()
    }

    val fallbackTitle = stringResource(R.string.course_content_title)
    val rawCourseName = (uiState as? CourseDetailUiState.Success)?.courseName.orEmpty()
    val courseTitle = remember(rawCourseName, fallbackTitle) {
        if (rawCourseName.isBlank()) {
            CourseTitle(code = null, name = fallbackTitle, teacher = null, academicYear = null)
        } else {
            CourseTitleParser.parse(rawCourseName)
        }
    }

    // Driven by the list itself (not nested-scroll offsets) so the bar is also
    // opaque when the list is restored already scrolled.
    val isContentScrolled by remember(lazyListState) {
        derivedStateOf {
            lazyListState.firstVisibleItemIndex > 0 || lazyListState.firstVisibleItemScrollOffset > 0
        }
    }
    val isHeaderHidden by remember(lazyListState) {
        derivedStateOf { lazyListState.firstVisibleItemIndex > 0 }
    }
    val isSuccess = uiState is CourseDetailUiState.Success

    Scaffold(
        modifier = modifier,
        containerColor = Color.Transparent,
        topBar = {
            CourseTopBar(
                title = courseTitle.name,
                showTitle = isSuccess && isHeaderHidden,
                isScrolled = isSuccess && isContentScrolled,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when (val state = uiState) {
                is CourseDetailUiState.Loading -> {
                    Column(
                        modifier = Modifier
                            .padding(top = innerPadding.calculateTopPadding())
                            .courseSharedBounds(viewModel.courseId)
                    ) {
                        ShimmerCourseList()
                    }
                }
                is CourseDetailUiState.Success -> {
                    if (state.sections.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Book,
                            title = stringResource(R.string.course_empty_title),
                            subtitle = stringResource(R.string.course_empty_subtitle),
                            actionLabel = stringResource(R.string.refresh),
                            onAction = { viewModel.loadContents() },
                            modifier = Modifier.courseSharedBounds(viewModel.courseId)
                        )
                    } else {
                        CourseContentList(
                            state = state,
                            courseTitle = courseTitle,
                            courseId = viewModel.courseId,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            lazyListState = lazyListState,
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding() + WeBeepSpacing.xs,
                                bottom = innerPadding.calculateBottomPadding() + WeBeepSpacing.lg
                            ),
                            onToggleSection = { viewModel.toggleSection(it) },
                            onPreviewFile = onPreviewFile,
                            onDownloadFile = { viewModel.downloadFile(it) },
                            onOpenFile = { viewModel.openFile(it) }
                        )
                    }
                }
                is CourseDetailUiState.Error -> {
                    EmptyStateView(
                        icon = Icons.Default.Description,
                        title = stringResource(R.string.sync_error_title),
                        subtitle = stringResource(state.messageRes),
                        actionLabel = stringResource(R.string.retry),
                        onAction = { viewModel.loadContents() }
                    )
                }
            }
        }
    }
}

/**
 * Top bar that is transparent over the header and becomes an opaque
 * surfaceContainer as soon as content scrolls underneath it, so text never
 * shows through. The short course name fades in once the header is gone.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CourseTopBar(
    title: String,
    showTitle: Boolean,
    isScrolled: Boolean,
    onBackClick: () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    val containerColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        animationSpec = WeBeepMotion.smooth(reduceMotion = reduceMotion),
        label = "top_bar_container"
    )
    TopAppBar(
        title = {
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)),
                exit = fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.cd_back)
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}

@Composable
private fun CourseContentList(
    state: CourseDetailUiState.Success,
    courseTitle: CourseTitle,
    courseId: Int,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
    onToggleSection: (Int) -> Unit,
    onPreviewFile: (FileUiModel) -> Unit,
    onDownloadFile: (FileUiModel) -> Unit,
    onOpenFile: (FileUiModel) -> Unit
) {
    val visibleSections = remember(state.sections, searchQuery) {
        filterSections(state.sections, searchQuery)
    }
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = MaxContentWidth),
            contentPadding = contentPadding,
            verticalArrangement = Arrangement.spacedBy(WeBeepSpacing.sm)
        ) {
            item(key = "course_header", contentType = "course_header") {
                CourseHeader(
                    title = courseTitle,
                    modifier = Modifier
                        .padding(horizontal = WeBeepSpacing.md)
                        .padding(top = WeBeepSpacing.xs)
                        .courseSharedBounds(courseId)
                )
            }
            item(key = "course_search", contentType = "course_search") {
                WeBeepSearchField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = stringResource(R.string.search_materials_placeholder),
                    modifier = Modifier.padding(
                        start = WeBeepSpacing.md,
                        end = WeBeepSpacing.md,
                        top = WeBeepSpacing.xs,
                        bottom = WeBeepSpacing.xxs
                    )
                )
            }
            if (visibleSections.isEmpty()) {
                item(key = "course_no_results", contentType = "course_no_results") {
                    EmptyStateView(
                        icon = Icons.Default.Description,
                        title = stringResource(R.string.no_materials_found),
                        subtitle = stringResource(R.string.try_changing_filters),
                        actionLabel = stringResource(R.string.reset_filters),
                        onAction = { onSearchQueryChange("") },
                        modifier = Modifier.height(EmptySearchHeight)
                    )
                }
            } else {
                items(
                    items = visibleSections,
                    key = { it.id },
                    contentType = { "section_item" }
                ) { section ->
                    SectionCard(
                        section = section,
                        onHeaderClick = { onToggleSection(section.id) },
                        onPreviewClick = onPreviewFile,
                        onDownloadClick = onDownloadFile,
                        onOpenClick = onOpenFile
                    )
                }
            }
        }
    }
}
