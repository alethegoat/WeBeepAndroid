package it.polimi.webeep.feature.course.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.draw.drawBehind
import androidx.compose.foundation.lazy.rememberLazyListState
import it.polimi.webeep.core.ui.util.ScrollBoundaryHaptics
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.ui.graphics.vector.ImageVector
import it.polimi.webeep.core.ui.theme.ColorExcel
import it.polimi.webeep.core.ui.theme.ColorFolder
import it.polimi.webeep.core.ui.theme.ColorGenericFile
import it.polimi.webeep.core.ui.theme.ColorLink
import it.polimi.webeep.core.ui.theme.ColorPdf
import it.polimi.webeep.core.ui.theme.ColorPowerPoint
import it.polimi.webeep.core.ui.theme.ColorWord
import it.polimi.webeep.core.ui.theme.ColorZip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.runtime.Composable
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.components.ShimmerCourseList
import it.polimi.webeep.core.ui.components.WeBeepSearchField
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.bouncyClickable
import it.polimi.webeep.core.ui.util.courseSharedBounds
import it.polimi.webeep.feature.download.presentation.DownloadProgressIndicator

/**
 * screen that shows the details of a course, its sections and materials.
 */
@OptIn(ExperimentalMaterial3Api::class)
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

    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

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
                            val courseName = (uiState as? CourseDetailUiState.Success)?.courseName ?: stringResource(R.string.course_content_title)
                            Text(
                                text = courseName,
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
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
                        containerColor = androidx.compose.ui.graphics.Color.Transparent,
                        scrolledContainerColor = androidx.compose.ui.graphics.Color.Transparent,
                        titleContentColor = MaterialTheme.colorScheme.onBackground,
                        navigationIconContentColor = MaterialTheme.colorScheme.onBackground
                    ),
                    scrollBehavior = scrollBehavior
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier.fillMaxSize()
            ) {
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
                    val visibleSections = remember(state.sections, searchQuery) {
                        filterSections(state.sections, searchQuery)
                    }
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
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            ScrollBoundaryHaptics(state = lazyListState)

                            LazyColumn(
                                state = lazyListState,
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .widthIn(max = 840.dp),
                                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                                    top = innerPadding.calculateTopPadding() + 8.dp,
                                    bottom = innerPadding.calculateBottomPadding() + 16.dp
                                ),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                            item {
                                val courseName = state.courseName ?: stringResource(R.string.course_content_title)
                                Column(
                                    modifier = Modifier
                                        .padding(horizontal = WeBeepSpacing.md)
                                        .courseSharedBounds(viewModel.courseId)
                                ) {
                                    Text(
                                        text = courseName,
                                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        modifier = Modifier.padding(vertical = WeBeepSpacing.md)
                                    )
                                    WeBeepSearchField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = stringResource(R.string.search_materials_placeholder),
                                        modifier = Modifier.padding(bottom = WeBeepSpacing.sm)
                                    )
                                }
                            }
                            
                            if (visibleSections.isEmpty()) {
                                item {
                                    EmptyStateView(
                                        icon = Icons.Default.Description,
                                        title = stringResource(R.string.no_materials_found),
                                        subtitle = stringResource(R.string.try_changing_filters),
                                        actionLabel = stringResource(R.string.reset_filters),
                                        onAction = { searchQuery = "" },
                                        modifier = Modifier.height(320.dp)
                                    )
                                }
                            } else {
                            items(
                                items = visibleSections,
                                key = { it.id },
                                contentType = { "section_item" }
                            ) { section ->
                                SectionItem(
                                    section = section,
                                    onHeaderClick = { viewModel.toggleSection(section.id) },
                                    onPreviewClick = onPreviewFile,
                                    onDownloadClick = { viewModel.downloadFile(it) },
                                    onOpenClick = { viewModel.openFile(it) }
                                )
                            }
                            }
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                            }
                        }
                    }
                }
                is CourseDetailUiState.Error -> {
                    EmptyStateView(
                        icon = Icons.Default.Description,
                        title = stringResource(R.string.sync_error_title),
                        subtitle = state.message,
                        actionLabel = stringResource(R.string.retry),
                        onAction = { viewModel.loadContents() }
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionItem(
    section: SectionUiModel,
    onHeaderClick: () -> Unit,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val rotationState by animateFloatAsState(
        targetValue = if (section.isExpanded) 180f else 0f,
        animationSpec = WeBeepMotion.smooth(),
        label = "arrow_rotation"
    )

    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // section header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .bouncyClickable { onHeaderClick() }
                    .padding(16.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = section.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!section.summary.isNullOrBlank()) {
                        val cleanSummary = remember(section.summary) {
                            it.polimi.webeep.core.util.MoodleText.cleanHtml(section.summary)
                        }
                        Text(
                            text = cleanSummary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = if (section.isExpanded) {
                        stringResource(R.string.cd_collapse_section)
                    } else {
                        stringResource(R.string.cd_expand_section)
                    },
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .graphicsLayer { rotationZ = rotationState }
                        .size(24.dp)
                )
            }

            AnimatedVisibility(
                visible = section.isExpanded,
                enter = expandVertically(animationSpec = WeBeepMotion.enter()) +
                    fadeIn(WeBeepMotion.enter()),
                exit = shrinkVertically(animationSpec = WeBeepMotion.exit()) +
                    fadeOut(WeBeepMotion.exit())
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (section.modules.isEmpty()) {
                        Text(
                            text = stringResource(R.string.no_materials_section),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    } else {
                        Spacer(modifier = Modifier.height(8.dp))
                        section.modules.forEachIndexed { index, module ->
                            ModuleItem(
                                module = module,
                                onPreviewClick = onPreviewClick,
                                onDownloadClick = onDownloadClick,
                                onOpenClick = onOpenClick
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun ModuleItem(
    module: ModuleUiModel,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val isFolder = module.type == "folder"
    val isSingleFile = module.type != "folder" && module.type !in setOf("url", "lti", "webex") && module.files.size == 1
    val isMultiFile = module.type != "folder" && module.type !in setOf("url", "lti", "webex") && module.files.size > 1

    val (moduleIcon, moduleColor) = when {
        isFolder -> Icons.Default.Folder to ColorFolder
        module.type in setOf("url", "lti", "webex") -> Icons.Default.Link to ColorLink
        isSingleFile -> getFileIconAndColor(module.files.first().filename)
        else -> Icons.Default.Description to ColorGenericFile
    }

    val isFolderExpanded = remember { androidx.compose.runtime.mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 16.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .let { m ->
                    if (isFolder) {
                        m.bouncyClickable { isFolderExpanded.value = !isFolderExpanded.value }
                    } else if (isSingleFile) {
                        m.bouncyClickable { onPreviewClick(module.files.first()) }
                    } else if (!module.url.isNullOrBlank() && module.type in setOf("url", "lti", "webex")) {
                        m.bouncyClickable {
                            onPreviewClick(
                                FileUiModel(
                                    filename = module.name,
                                    filepath = "/",
                                    localFileName = "",
                                    fileUrl = module.url,
                                    fileSize = 0L,
                                    mimeType = "text/html",
                                    formattedSize = ""
                                )
                            )
                        }
                    } else {
                        m
                    }
                }
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(moduleColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = moduleIcon,
                    contentDescription = null,
                    tint = moduleColor,
                    modifier = Modifier.size(28.dp)
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = module.name,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            
            if (isFolder) {
                val rotation by animateFloatAsState(
                    if (isFolderExpanded.value) 180f else 0f,
                    animationSpec = WeBeepMotion.smooth(),
                    label = "rot"
                )
                Icon(
                    imageVector = Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.graphicsLayer { rotationZ = rotation }
                )
            } else if (isSingleFile) {
                val file = module.files.first()
                DownloadProgressIndicator(
                    state = file.downloadState,
                    onDownloadClick = { onDownloadClick(file) },
                    onOpenClick = { onOpenClick(file) }
                )
            } else if (!module.url.isNullOrBlank() && module.type in setOf("url", "lti", "webex")) {
                Icon(
                    imageVector = Icons.Default.Visibility,
                    contentDescription = stringResource(R.string.cd_preview),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        if (!module.description.isNullOrBlank()) {
            val cleanDescription = remember(module.description) {
                it.polimi.webeep.core.util.MoodleText.cleanHtml(module.description)
            }
            Text(
                text = cleanDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 56.dp, top = 4.dp, bottom = 8.dp)
            )
        }

        val showTree = (isFolder && isFolderExpanded.value) || isMultiFile
        AnimatedVisibility(
            visible = showTree,
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
        ) {
            val rootNode = remember(module.files) {
                val root = FolderNode("", "")
                module.files.forEach { file ->
                    val segments = file.filepath.trim('/').split('/').filter { it.isNotEmpty() }
                    var current = root
                    var pathAccumulator = ""
                    for (segment in segments) {
                        pathAccumulator += "/$segment"
                        if (!current.subfolders.containsKey(segment)) {
                            current.subfolders[segment] = FolderNode(segment, pathAccumulator)
                        }
                        current = current.subfolders.getValue(segment)
                    }
                    current.files.add(file)
                }
                root
            }
            val expandedFolders = remember { androidx.compose.runtime.mutableStateMapOf<String, Boolean>() }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 56.dp, top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                    .padding(vertical = 8.dp)
            ) {
                FolderTreeView(
                    node = rootNode,
                    expandedFolders = expandedFolders,
                    isRoot = true,
                    onPreviewClick = onPreviewClick,
                    onDownloadClick = onDownloadClick,
                    onOpenClick = onOpenClick
                )
            }
        }
    }
}

private fun getFileIconAndColor(filename: String): Pair<ImageVector, Color> {
    val ext = filename.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> Icons.Default.PictureAsPdf to ColorPdf
        "doc", "docx" -> Icons.Default.Description to ColorWord
        "xls", "xlsx" -> Icons.Default.TableChart to ColorExcel
        "ppt", "pptx" -> Icons.Default.Slideshow to ColorPowerPoint
        "zip", "rar", "7z" -> Icons.Default.FolderZip to ColorZip
        else -> Icons.Default.Description to ColorGenericFile
    }
}

private data class FolderNode(
    val name: String,
    val fullPath: String,
    val files: MutableList<FileUiModel> = mutableListOf(),
    val subfolders: MutableMap<String, FolderNode> = mutableMapOf()
)

@Composable
private fun FolderTreeView(
    node: FolderNode,
    expandedFolders: androidx.compose.runtime.snapshots.SnapshotStateMap<String, Boolean>,
    isRoot: Boolean,
    depth: Int = 0,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit
) {
    if (!isRoot) {
        val isExpanded = expandedFolders[node.fullPath] ?: false
        val rotation by animateFloatAsState(targetValue = if (isExpanded) 180f else 0f, label = "rot")

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .bouncyClickable { expandedFolders[node.fullPath] = !isExpanded }
                .padding(start = (12 + depth * 16).dp, top = 12.dp, bottom = 12.dp, end = 16.dp)
        ) {
            Icon(Icons.Default.Folder, null, tint = ColorFolder, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(12.dp))
            Text(
                text = node.name,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Icon(Icons.Default.ExpandMore, null, modifier = Modifier.graphicsLayer { rotationZ = rotation }.size(20.dp), tint = MaterialTheme.colorScheme.onSurface)
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeIn(),
            exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessMedium)) + fadeOut()
        ) {
            Column {
                node.files.forEach { file ->
                    FileRowItem(file, startPadding = (48 + depth * 16).dp, onPreviewClick, onDownloadClick, onOpenClick)
                }
                node.subfolders.values.sortedBy { it.name }.forEach { subNode ->
                    FolderTreeView(subNode, expandedFolders, false, depth + 1, onPreviewClick, onDownloadClick, onOpenClick)
                }
            }
        }
    } else {
        node.files.forEachIndexed { index, file ->
            FileRowItem(file, startPadding = 12.dp, onPreviewClick, onDownloadClick, onOpenClick)
        }
        val sortedSubfolders = node.subfolders.values.sortedBy { it.name }
        sortedSubfolders.forEachIndexed { index, subNode ->
            FolderTreeView(subNode, expandedFolders, false, 0, onPreviewClick, onDownloadClick, onOpenClick)
        }
    }
}

@Composable
private fun FileRowItem(
    file: FileUiModel,
    startPadding: androidx.compose.ui.unit.Dp,
    onPreviewClick: (FileUiModel) -> Unit,
    onDownloadClick: (FileUiModel) -> Unit,
    onOpenClick: (FileUiModel) -> Unit,
    modifier: Modifier = Modifier
) {
    val (fileIcon, fileColor) = getFileIconAndColor(file.filename)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .bouncyClickable { onPreviewClick(file) }
            .padding(start = startPadding, top = 12.dp, bottom = 12.dp, end = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(fileColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = fileIcon,
                contentDescription = null,
                tint = fileColor,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.filename,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = file.formattedSize,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DownloadProgressIndicator(
            state = file.downloadState,
            onDownloadClick = { onDownloadClick(file) },
            onOpenClick = { onOpenClick(file) }
        )
    }
}

