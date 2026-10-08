package it.polimi.webeep.feature.course.presentation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.Description
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
import androidx.compose.runtime.snapshotFlow
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
import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.core.ui.components.GroupedListGap
import it.polimi.webeep.core.ui.components.GroupedListSectionGap
import it.polimi.webeep.core.ui.components.ShimmerCourseSections
import it.polimi.webeep.core.ui.components.WeBeepSearchField
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.courseSharedBounds
import it.polimi.webeep.core.ui.util.rememberNavTransitionRunning
import it.polimi.webeep.core.util.CourseTitle
import it.polimi.webeep.core.util.CourseTitleParser
import kotlinx.coroutines.flow.filter

private val MaxContentWidth = 840.dp
private val EmptySearchHeight = 320.dp
private val EmptyListContent = CourseListContent(items = emptyList(), noMatches = false)

@Composable
fun CourseDetailScreen(
    viewModel: CourseDetailViewModel,
    onPreviewFile: (FileUiModel) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val listContent by viewModel.listContent.collectAsStateWithLifecycle()
    val reduceMotion = LocalReduceMotion.current
    val lazyListState = rememberLazyListState()
    val keyboard = LocalSoftwareKeyboardController.current
    // Local state keeps typing synchronous; the ViewModel filters off the main thread.
    var searchQuery by rememberSaveable { mutableStateOf(viewModel.searchQuery.value) }
    val onSearchQueryChange: (String) -> Unit = remember(viewModel) {
        { query ->
            searchQuery = query
            viewModel.setSearchQuery(query)
        }
    }
    val isNavTransitionRunning = rememberNavTransitionRunning()

    // Collected, not read in composition: scrolling never recomposes the screen.
    LaunchedEffect(lazyListState, keyboard) {
        snapshotFlow { lazyListState.isScrollInProgress }
            .filter { it }
            .collect { keyboard?.hide() }
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
    // Still "loading" until the rows for these contents are built, so the list
    // never shows up empty for a frame.
    val displayState = when (val state = uiState) {
        is CourseDetailUiState.Success -> if (listContent == null) CourseDetailUiState.Loading else state
        else -> state
    }
    val isSuccess = displayState is CourseDetailUiState.Success

    Scaffold(
        // One stable, opaque container for the card -> screen transform, whatever the state.
        modifier = modifier.courseSharedBounds(viewModel.courseId, isScreen = true),
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            CourseTopBar(
                title = courseTitle.name,
                showTitle = isSuccess && isHeaderHidden,
                isScrolled = isSuccess && isContentScrolled,
                onBackClick = onBackClick
            )
        }
    ) { innerPadding ->
        AnimatedContent(
            targetState = displayState,
            contentKey = { it::class },
            transitionSpec = {
                if (isNavTransitionRunning()) {
                    // Contents landing mid navigation swap in at once: one animation at a time.
                    EnterTransition.None togetherWith ExitTransition.None
                } else {
                    fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)) togetherWith
                        fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
                }
            },
            label = "course_state",
            modifier = Modifier.fillMaxSize()
        ) { state ->
            when (state) {
                is CourseDetailUiState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        ShimmerCourseSections(
                            modifier = Modifier
                                .padding(top = innerPadding.calculateTopPadding())
                                .widthIn(max = MaxContentWidth)
                        )
                    }
                }
                is CourseDetailUiState.Success -> {
                    if (state.sections.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Rounded.Book,
                            title = stringResource(R.string.course_empty_title),
                            subtitle = stringResource(R.string.course_empty_subtitle),
                            actionLabel = stringResource(R.string.refresh),
                            onAction = { viewModel.loadContents() }
                        )
                    } else {
                        CourseContentList(
                            content = listContent ?: EmptyListContent,
                            courseTitle = courseTitle,
                            searchQuery = searchQuery,
                            onSearchQueryChange = onSearchQueryChange,
                            lazyListState = lazyListState,
                            contentPadding = PaddingValues(
                                top = innerPadding.calculateTopPadding() + WeBeepSpacing.xs,
                                bottom = innerPadding.calculateBottomPadding() + WeBeepSpacing.lg
                            ),
                            onToggleSection = viewModel::toggleSection,
                            onToggleFolder = viewModel::toggleFolder,
                            onPreviewFile = onPreviewFile,
                            onDownloadFile = viewModel::downloadFile,
                            onOpenFile = viewModel::openFile
                        )
                    }
                }
                is CourseDetailUiState.Error -> {
                    EmptyStateView(
                        icon = Icons.Rounded.Description,
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
                    imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
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
    content: CourseListContent,
    courseTitle: CourseTitle,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    lazyListState: LazyListState,
    contentPadding: PaddingValues,
    onToggleSection: (Int) -> Unit,
    onToggleFolder: (String) -> Unit,
    onPreviewFile: (FileUiModel) -> Unit,
    onDownloadFile: (FileUiModel) -> Unit,
    onOpenFile: (FileUiModel) -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
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
            verticalArrangement = Arrangement.spacedBy(GroupedListGap)
        ) {
            item(key = "course_header", contentType = "course_header") {
                CourseHeader(
                    title = courseTitle,
                    modifier = Modifier
                        .padding(horizontal = WeBeepSpacing.md)
                        .padding(top = WeBeepSpacing.xs)
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
                        top = WeBeepSpacing.sm,
                        bottom = WeBeepSpacing.xxs
                    )
                )
            }
            if (content.noMatches) {
                item(key = "course_no_results", contentType = "course_no_results") {
                    EmptyStateView(
                        icon = Icons.Rounded.Description,
                        title = stringResource(R.string.no_materials_found),
                        subtitle = stringResource(R.string.try_changing_filters),
                        actionLabel = stringResource(R.string.reset_filters),
                        onAction = { onSearchQueryChange("") },
                        modifier = Modifier.height(EmptySearchHeight)
                    )
                }
            } else {
                items(
                    items = content.items,
                    key = { it.key },
                    contentType = { it.contentType }
                ) { item ->
                    val startsGroup = item.position == GroupPosition.Top || item.position == GroupPosition.Single
                    val rowModifier = Modifier
                        .then(
                            if (reduceMotion) {
                                Modifier
                            } else {
                                // Expanding/collapsing inserts or removes rows: they fade, the rest glide.
                                Modifier.animateItem(
                                    fadeInSpec = WeBeepMotion.enter(),
                                    fadeOutSpec = WeBeepMotion.exit(),
                                    placementSpec = WeBeepMotion.spatialSpring()
                                )
                            }
                        )
                        .padding(horizontal = WeBeepSpacing.md)
                        .padding(top = if (startsGroup) GroupedListSectionGap else 0.dp)
                    CourseListRow(
                        item = item,
                        onToggleSection = onToggleSection,
                        onToggleFolder = onToggleFolder,
                        onPreviewFile = onPreviewFile,
                        onDownloadFile = onDownloadFile,
                        onOpenFile = onOpenFile,
                        modifier = rowModifier
                    )
                }
            }
        }
    }
}

@Composable
private fun CourseListRow(
    item: CourseListItem,
    onToggleSection: (Int) -> Unit,
    onToggleFolder: (String) -> Unit,
    onPreviewFile: (FileUiModel) -> Unit,
    onDownloadFile: (FileUiModel) -> Unit,
    onOpenFile: (FileUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    when (item) {
        is CourseListItem.SectionHeader -> SectionHeaderRow(
            item = item,
            onClick = { onToggleSection(item.sectionId) },
            modifier = modifier
        )
        is CourseListItem.SectionEmpty -> SectionEmptyRow(item = item, modifier = modifier)
        is CourseListItem.Module -> ModuleItemRow(
            item = item,
            onToggleFolder = onToggleFolder,
            onPreviewClick = onPreviewFile,
            onDownloadClick = onDownloadFile,
            onOpenClick = onOpenFile,
            modifier = modifier
        )
        is CourseListItem.Subfolder -> SubfolderItemRow(
            item = item,
            onToggleFolder = onToggleFolder,
            modifier = modifier
        )
        is CourseListItem.File -> FileItemRow(
            item = item,
            onPreviewClick = onPreviewFile,
            onDownloadClick = onDownloadFile,
            onOpenClick = onOpenFile,
            modifier = modifier
        )
    }
}
