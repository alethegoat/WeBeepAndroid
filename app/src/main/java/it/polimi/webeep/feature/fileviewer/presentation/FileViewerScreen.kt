package it.polimi.webeep.feature.fileviewer.presentation

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.outlined.BrokenImage
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.DownloadDone
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.rememberZoomState
import it.polimi.webeep.core.ui.util.zoomable
import it.polimi.webeep.core.util.FileUtils
import it.polimi.webeep.feature.course.presentation.DownloadState
import it.polimi.webeep.feature.fileviewer.data.FileContentState
import it.polimi.webeep.feature.fileviewer.domain.ViewerFile
import it.polimi.webeep.feature.fileviewer.presentation.components.CodeViewer
import it.polimi.webeep.feature.fileviewer.presentation.components.MediaViewer
import it.polimi.webeep.feature.fileviewer.presentation.components.PdfViewer
import java.io.File
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val TopBarHeight = 64.dp
private val ActionProgressSize = 20.dp
private val ActionProgressStroke = 2.dp

/**
 * Full-screen file viewer. The top bar floats over the content (tap toggles it
 * and the system bars), so hiding it never moves the page. When opened from a
 * course, sibling files of the same folder can be swiped through.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FileViewerScreen(viewModel: FileViewerViewModel, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val viewerFiles by viewModel.files.collectAsStateWithLifecycle()
    var chromeVisible by rememberSaveable { mutableStateOf(true) }
    val toggleChrome = { chromeVisible = !chromeVisible }

    SystemBarsVisibility(visible = chromeVisible)

    // Ignoring visibility keeps the padding stable while the bars hide/show.
    val topPadding = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding() + TopBarHeight
    val bottomPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(top = topPadding, bottom = bottomPadding)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val files = viewerFiles
        if (files == null) {
            FileViewerLoading(title = viewModel.initialFile.title, progress = null)
            ViewerTopBar(
                visible = chromeVisible,
                file = viewModel.initialFile,
                subtitle = null,
                state = null,
                viewModel = viewModel,
                onBackClick = onBackClick
            )
            return@Box
        }

        val pagerState = rememberPagerState(initialPage = files.initialIndex) { files.files.size }
        var currentPageZoomed by remember { mutableStateOf(false) }

        LaunchedEffect(pagerState) {
            snapshotFlow { pagerState.settledPage }.collect { page ->
                currentPageZoomed = false
                viewModel.onPageSettled(page)
            }
        }

        HorizontalPager(
            state = pagerState,
            key = { files.files[it].url },
            beyondViewportPageCount = 0,
            pageSpacing = 16.dp,
            userScrollEnabled = files.files.size > 1 && !currentPageZoomed,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val file = files.files[page]
            val isCurrent = pagerState.settledPage == page
            FileViewerPage(
                file = file,
                viewModel = viewModel,
                isCurrentPage = isCurrent,
                contentPadding = contentPadding,
                onToggleChrome = toggleChrome,
                onZoomedChange = { zoomed -> if (isCurrent) currentPageZoomed = zoomed }
            )
        }

        val currentFile = files.files[pagerState.currentPage.coerceIn(0, files.files.lastIndex)]
        val currentState by remember(currentFile) { viewModel.contentFor(currentFile) }
            .collectAsStateWithLifecycle()
        val position = if (files.files.size > 1) {
            stringResource(R.string.file_viewer_position, pagerState.currentPage + 1, files.files.size)
        } else {
            null
        }
        val kind = when (val state = currentState) {
            is FileContentState.Pdf -> pluralStringResource(R.plurals.file_viewer_pdf_pages, state.pageCount, state.pageCount)
            is FileContentState.Image -> stringResource(R.string.file_viewer_image)
            else -> null
        }

        ViewerTopBar(
            visible = chromeVisible,
            file = currentFile,
            subtitle = listOfNotNull(position, kind).joinToString(" · ").ifEmpty { null },
            state = currentState,
            viewModel = viewModel,
            onBackClick = onBackClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ViewerTopBar(
    visible: Boolean,
    file: ViewerFile,
    subtitle: String?,
    state: FileContentState?,
    viewModel: FileViewerViewModel,
    onBackClick: () -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(WeBeepMotion.enter(reduceMotion = reduceMotion)) { -it } +
            fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)),
        exit = slideOutVertically(WeBeepMotion.exit(reduceMotion = reduceMotion)) { -it } +
            fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = file.title,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (subtitle != null) {
                        Text(
                            text = subtitle,
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
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
            actions = {
                val canAct = state != null &&
                    state !is FileContentState.Downloading &&
                    state !is FileContentState.Error
                if (canAct) {
                    FileActions(file = file, viewModel = viewModel)
                }
            },
            // Opaque: pages must never show through the title.
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        )
    }
}

@Composable
private fun FileActions(file: ViewerFile, viewModel: FileViewerViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preparingShare by viewModel.preparingShare.collectAsStateWithLifecycle()
    val downloadStates by viewModel.downloadStates.collectAsStateWithLifecycle()
    val downloadState = remember(file, downloadStates) { viewModel.downloadStateFor(file) }
    var menuOpen by remember { mutableStateOf(false) }
    val shareChooserTitle = stringResource(R.string.file_viewer_share_chooser)
    val openWithChooserTitle = stringResource(R.string.file_viewer_open_with)

    fun withLocalFile(action: (File) -> Unit) {
        scope.launch { viewModel.localFileFor(file)?.let(action) }
    }

    IconButton(
        onClick = {
            withLocalFile { local ->
                shareFile(context, local, viewModel.shareMimeType(file), file.title, shareChooserTitle)
            }
        },
        enabled = !preparingShare
    ) {
        if (preparingShare) {
            CircularProgressIndicator(modifier = Modifier.size(ActionProgressSize), strokeWidth = ActionProgressStroke)
        } else {
            Icon(Icons.Rounded.Share, contentDescription = stringResource(R.string.file_viewer_share))
        }
    }

    when (downloadState) {
        DownloadState.DOWNLOADING -> Box(Modifier.size(WeBeepSpacing.touchTarget), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(modifier = Modifier.size(ActionProgressSize), strokeWidth = ActionProgressStroke)
        }
        DownloadState.COMPLETED -> Box(Modifier.size(WeBeepSpacing.touchTarget), contentAlignment = Alignment.Center) {
            Icon(
                Icons.Rounded.DownloadDone,
                contentDescription = stringResource(R.string.file_viewer_downloaded),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        DownloadState.NOT_STARTED, DownloadState.FAILED -> IconButton(onClick = { viewModel.download(file) }) {
            Icon(
                imageVector = if (downloadState == DownloadState.FAILED) Icons.Rounded.ErrorOutline else Icons.Rounded.Download,
                contentDescription = stringResource(R.string.file_viewer_download)
            )
        }
    }

    Box {
        IconButton(onClick = { menuOpen = true }) {
            Icon(Icons.Rounded.MoreVert, contentDescription = stringResource(R.string.file_viewer_more_options))
        }
        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
            DropdownMenuItem(
                text = { Text(stringResource(R.string.file_viewer_open_with)) },
                leadingIcon = { Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null) },
                onClick = {
                    menuOpen = false
                    withLocalFile { local ->
                        openWithExternalApp(context, local, viewModel.shareMimeType(file), openWithChooserTitle)
                    }
                }
            )
        }
    }
}

@Composable
private fun FileViewerPage(
    file: ViewerFile,
    viewModel: FileViewerViewModel,
    isCurrentPage: Boolean,
    contentPadding: PaddingValues,
    onToggleChrome: () -> Unit,
    onZoomedChange: (Boolean) -> Unit
) {
    val reduceMotion = LocalReduceMotion.current
    var attempt by remember { mutableIntStateOf(0) }
    val content by remember(file, attempt) { viewModel.contentFor(file) }.collectAsStateWithLifecycle()

    AnimatedContent(
        targetState = content,
        contentKey = { it::class },
        transitionSpec = {
            (
                fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)) +
                    scaleIn(WeBeepMotion.enter(reduceMotion = reduceMotion), initialScale = 0.98f)
                ) togetherWith
                fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
        },
        label = "file_content",
        modifier = Modifier.fillMaxSize()
    ) { state ->
        when (state) {
            is FileContentState.Downloading -> FileViewerLoading(title = file.title, progress = state)
            is FileContentState.Image -> ImagePreviewContent(
                url = state.url,
                title = file.title,
                onToggleFullScreen = onToggleChrome,
                onZoomedChange = onZoomedChange
            )
            is FileContentState.Pdf -> PdfViewer(
                file = state.file,
                pageCount = state.pageCount,
                contentPadding = contentPadding,
                onToggleFullScreen = onToggleChrome,
                onZoomedChange = onZoomedChange
            )
            is FileContentState.Media -> MediaViewer(
                url = state.url,
                callFactory = viewModel.mediaCallFactory,
                isCurrentPage = isCurrentPage,
                modifier = Modifier.padding(top = contentPadding.calculateTopPadding())
            )
            is FileContentState.Code -> CodeViewer(
                content = state.content,
                contentPadding = contentPadding,
                onTap = onToggleChrome
            )
            is FileContentState.External -> ExternalFileContent(
                file = state.file,
                mimeType = state.mimeType,
                title = file.title,
                autoLaunch = remember(file) { viewModel.consumeAutoLaunch(file) }
            )
            is FileContentState.Error -> EmptyStateView(
                icon = Icons.Rounded.Description,
                title = stringResource(R.string.file_viewer_error_title),
                subtitle = stringResource(state.messageRes),
                actionLabel = stringResource(R.string.retry),
                onAction = {
                    viewModel.retry(file)
                    attempt++
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/** Hides status and navigation bars while the viewer chrome is hidden; swipe brings them back. */
@Composable
private fun SystemBarsVisibility(visible: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val window = view.context.findActivity()?.window ?: return
    DisposableEffect(window, visible) {
        val controller = WindowCompat.getInsetsController(window, view)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (visible) {
            controller.show(WindowInsetsCompat.Type.systemBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.systemBars())
        }
        onDispose { controller.show(WindowInsetsCompat.Type.systemBars()) }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

@Composable
private fun FileViewerLoading(title: String, progress: FileContentState.Downloading?) {
    val reduceMotion = LocalReduceMotion.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WeBeepSpacing.xl),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = WeBeepSpacing.lg)
        )
        val fraction = progress?.progress
        if (fraction != null) {
            val animated by animateFloatAsState(
                targetValue = fraction,
                animationSpec = WeBeepMotion.smooth(durationMs = WeBeepMotion.SHORT_MS, reduceMotion = reduceMotion),
                label = "download_progress"
            )
            LinearProgressIndicator(progress = { animated }, modifier = Modifier.fillMaxWidth())
        } else {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
        val total = progress?.totalBytes
        val read = progress?.bytesRead ?: 0L
        Text(
            text = when {
                total != null && read > 0 -> stringResource(
                    R.string.file_viewer_download_progress,
                    FileUtils.formatFileSize(read),
                    FileUtils.formatFileSize(total)
                )
                read > 0 -> FileUtils.formatFileSize(read)
                else -> stringResource(R.string.file_viewer_loading)
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = WeBeepSpacing.md),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ImagePreviewContent(
    url: String,
    title: String,
    modifier: Modifier = Modifier,
    onToggleFullScreen: () -> Unit = {},
    onZoomedChange: (Boolean) -> Unit = {}
) {
    val reduceMotion = LocalReduceMotion.current
    val zoomState = rememberZoomState()
    val coroutineScope = rememberCoroutineScope()
    var showHint by remember { mutableStateOf(true) }

    LaunchedEffect(zoomState.isZoomed) { onZoomedChange(zoomState.isZoomed) }
    LaunchedEffect(Unit) {
        delay(HINT_DURATION_MS)
        showHint = false
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(zoomState) {
                detectTapGestures(
                    onTap = { onToggleFullScreen() },
                    onDoubleTap = { tap ->
                        coroutineScope.launch {
                            zoomState.toggleZoom(tap, WeBeepMotion.smooth(reduceMotion = reduceMotion))
                        }
                    }
                )
            }
    ) {
        SubcomposeAsyncImage(
            model = url,
            contentDescription = title,
            modifier = Modifier
                .fillMaxSize()
                .zoomable(zoomState),
            contentScale = ContentScale.Fit,
            loading = {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            },
            error = {
                EmptyStateView(
                    icon = Icons.Outlined.BrokenImage,
                    title = stringResource(R.string.file_viewer_error_title),
                    subtitle = stringResource(R.string.file_viewer_image_error),
                    modifier = Modifier.fillMaxSize()
                )
            },
            success = { SubcomposeAsyncImageContent() }
        )

        AnimatedVisibility(
            visible = showHint && !zoomState.isZoomed,
            enter = fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)),
            exit = fadeOut(WeBeepMotion.exit(durationMs = WeBeepMotion.MEDIUM_MS, reduceMotion = reduceMotion)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(WeBeepSpacing.md)
        ) {
            Surface(
                shape = CircleShape,
                color = WeBeepTheme.colors.mediaScrim.copy(alpha = 0.65f),
                contentColor = WeBeepTheme.colors.onMedia
            ) {
                Text(
                    text = stringResource(R.string.file_viewer_pinch_hint),
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = WeBeepSpacing.md, vertical = WeBeepSpacing.xs)
                )
            }
        }
    }
}

private const val HINT_DURATION_MS = 2_000L

@Composable
private fun ExternalFileContent(file: File, mimeType: String, title: String, autoLaunch: Boolean) {
    val context = LocalContext.current
    val chooserTitle = stringResource(R.string.file_viewer_open_with)
    val launch = { openWithExternalApp(context, file, mimeType, chooserTitle) }

    // Only the file the viewer was opened on opens by itself, and only once
    // (not again on rotation or when swiping back to it).
    LaunchedEffect(file) {
        if (autoLaunch) launch()
    }

    EmptyStateView(
        icon = Icons.Rounded.Description,
        title = stringResource(R.string.file_viewer_external_title),
        subtitle = stringResource(R.string.file_viewer_external_subtitle, title),
        actionLabel = stringResource(R.string.file_viewer_open_again),
        onAction = { launch() },
        modifier = Modifier.fillMaxSize()
    )
}
