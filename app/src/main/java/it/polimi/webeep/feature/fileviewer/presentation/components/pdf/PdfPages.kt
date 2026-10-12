package it.polimi.webeep.feature.fileviewer.presentation.components.pdf

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.ZoomState
import it.polimi.webeep.core.ui.util.rememberZoomState
import it.polimi.webeep.core.ui.util.zoomable
import kotlin.math.abs
import kotlinx.coroutines.launch

/**
 * Continuous page list. Pinch zooms around the fingers; while zoomed, vertical
 * pan past the page edge keeps scrolling through pages. [contentPadding] keeps
 * the first page clear of the overlaid top bar without moving when it hides.
 */
@Composable
internal fun PdfPages(
    session: PdfRendererSession,
    pageCount: Int,
    defaultAspectRatio: Float,
    highResWidth: Int,
    onToggleFullScreen: () -> Unit,
    onZoomedChange: (Boolean) -> Unit,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val reduceMotion = LocalReduceMotion.current
    val zoomState = rememberZoomState()
    // rememberLazyListState is saveable: the page survives rotation.
    val lazyListState = rememberLazyListState()
    val currentPage by rememberCurrentPage(lazyListState)
    var showGoToPage by remember { mutableStateOf(false) }

    val currentOnZoomedChange by rememberUpdatedState(onZoomedChange)
    LaunchedEffect(zoomState.isZoomed) { currentOnZoomedChange(zoomState.isZoomed) }
    LaunchedEffect(currentPage) { session.prefetchAround(currentPage, pageCount) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
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
        PdfPageList(
            session = session,
            pageCount = pageCount,
            defaultAspectRatio = defaultAspectRatio,
            highResWidth = highResWidth,
            lazyListState = lazyListState,
            zoomState = zoomState,
            contentPadding = contentPadding
        )
        PdfPageIndicator(
            currentPage = currentPage,
            pageCount = pageCount,
            isScrolling = lazyListState.isScrollInProgress,
            onClick = { showGoToPage = true },
            modifier = Modifier.align(Alignment.BottomEnd)
        )
    }

    if (showGoToPage) {
        GoToPageDialog(
            pageCount = pageCount,
            onDismiss = { showGoToPage = false },
            onGo = { page ->
                showGoToPage = false
                coroutineScope.launch {
                    zoomState.reset()
                    if (reduceMotion) lazyListState.scrollToItem(page) else lazyListState.animateScrollToItem(page)
                }
            }
        )
    }
}

@Composable
private fun PdfPageList(
    session: PdfRendererSession,
    pageCount: Int,
    defaultAspectRatio: Float,
    highResWidth: Int,
    lazyListState: LazyListState,
    zoomState: ZoomState,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()
    val flingBehavior = ScrollableDefaults.flingBehavior()

    LazyColumn(
        state = lazyListState,
        contentPadding = PaddingValues(
            start = WeBeepSpacing.xs,
            end = WeBeepSpacing.xs,
            top = contentPadding.calculateTopPadding() + WeBeepSpacing.xs,
            bottom = contentPadding.calculateBottomPadding() + PageIndicatorClearance
        ),
        verticalArrangement = Arrangement.spacedBy(WeBeepSpacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .zoomable(
                state = zoomState,
                onVerticalOverflow = { dy -> lazyListState.dispatchRawDelta(-dy) },
                onVerticalFling = { velocityY ->
                    coroutineScope.launch {
                        lazyListState.scroll {
                            with(flingBehavior) { performFling(-velocityY) }
                        }
                    }
                }
            )
    ) {
        items(
            count = pageCount,
            key = { index -> index },
            contentType = { "pdf_page" }
        ) { pageIndex ->
            PdfPageItem(
                session = session,
                pageIndex = pageIndex,
                defaultAspectRatio = defaultAspectRatio,
                highResWidth = highResWidth
            )
        }
    }
}

/** The page whose centre is closest to the viewport centre. */
@Composable
private fun rememberCurrentPage(lazyListState: LazyListState): State<Int> = remember(lazyListState) {
    derivedStateOf {
        val layoutInfo = lazyListState.layoutInfo
        val viewportCenter = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2
        layoutInfo.visibleItemsInfo
            .minByOrNull { abs(it.offset + (it.size / 2) - viewportCenter) }
            ?.index ?: 0
    }
}
