package it.polimi.webeep.feature.fileviewer.presentation.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.ScrollableDefaults
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import it.polimi.webeep.core.ui.util.rememberZoomState
import it.polimi.webeep.core.ui.util.zoomable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.components.EmptyStateView
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import kotlin.math.min
import kotlin.math.roundToInt

@Composable
fun PdfViewer(
    file: File,
    pageCount: Int,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(),
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    onToggleFullScreen: () -> Unit = {},
    onZoomedChange: (Boolean) -> Unit = {}
) {
    val pdfMutex = remember { Mutex() }

    // Small hard-capped cache. Never recycle evicted bitmaps here: a composed
    // Image may still be drawing them; recycle only when leaving composition.
    val memoryCache = remember {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = (maxMemory / 10)
            .coerceAtLeast(MIN_CACHE_SIZE_KB)
            .coerceAtMost(MAX_CACHE_SIZE_KB)
        object : android.util.LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024
            }
        }
    }

    // HD pages never exceed the screen width: a full 1800 px ARGB page is ~18 MB.
    val screenWidthPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.roundToPx() }
    val highResWidth = remember(screenWidthPx) { min(MAX_HIGH_RES_WIDTH, screenWidthPx) }

    val loadState by produceState<PdfLoadState>(PdfLoadState.Loading, file) {
        value = PdfLoadState.Loading
        var session: PdfSession? = null
        try {
            val opened = withContext(ioDispatcher) {
                // Tracked before returning so a cancellation here still closes it.
                pdfMutex.withLock { openPdfSession(file) }.also { session = it?.session }
            }
            value = if (opened != null) PdfLoadState.Ready(opened.session, opened.firstPageAspectRatio) else PdfLoadState.Error
            awaitCancellation()
        } finally {
            val toClose = session
            withContext(NonCancellable + ioDispatcher) {
                pdfMutex.withLock {
                    memoryCache.evictAll()
                    toClose?.close()
                }
            }
        }
    }

    when (val state = loadState) {
        PdfLoadState.Loading -> Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        PdfLoadState.Error -> EmptyStateView(
            icon = Icons.Default.Description,
            title = stringResource(R.string.file_viewer_error_title),
            subtitle = stringResource(R.string.file_viewer_error_pdf),
            modifier = modifier.fillMaxSize()
        )
        is PdfLoadState.Ready -> PdfPages(
            pdfRenderer = state.session.renderer,
            pageCount = pageCount,
            defaultAspectRatio = state.defaultAspectRatio,
            highResWidth = highResWidth,
            memoryCache = memoryCache,
            pdfMutex = pdfMutex,
            ioDispatcher = ioDispatcher,
            onToggleFullScreen = onToggleFullScreen,
            onZoomedChange = onZoomedChange,
            contentPadding = contentPadding,
            modifier = modifier
        )
    }
}

/**
 * Continuous page list. Pinch zooms around the fingers; while zoomed, vertical
 * pan past the page edge keeps scrolling through pages. [contentPadding] keeps
 * the first page clear of the overlaid top bar without moving when it hides.
 */
@Composable
private fun PdfPages(
    pdfRenderer: PdfRenderer,
    pageCount: Int,
    defaultAspectRatio: Float,
    highResWidth: Int,
    memoryCache: android.util.LruCache<String, Bitmap>,
    pdfMutex: Mutex,
    ioDispatcher: CoroutineDispatcher,
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
    val flingBehavior = ScrollableDefaults.flingBehavior()

    LaunchedEffect(zoomState.isZoomed) { onZoomedChange(zoomState.isZoomed) }

    val currentPage by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            if (visibleItemsInfo.isEmpty()) {
                0
            } else {
                val viewportCenter = (layoutInfo.viewportEndOffset + layoutInfo.viewportStartOffset) / 2
                val closestItem = visibleItemsInfo.minByOrNull {
                    val itemCenter = it.offset + (it.size / 2)
                    kotlin.math.abs(itemCenter - viewportCenter)
                }
                closestItem?.index ?: 0
            }
        }
    }

    // Prefetch neighbours of the current page.
    LaunchedEffect(currentPage) {
        val pagesToFetch = listOf(currentPage + 1, currentPage - 1).filter { it in 0 until pageCount }
        pagesToFetch.forEach { pageIndex ->
            launch(ioDispatcher) {
                val fastKey = "fast_$pageIndex"
                if (memoryCache.get(fastKey) == null) {
                    pdfMutex.withLock {
                        if (memoryCache.get(fastKey) != null || !isActive) return@withLock
                        try {
                            memoryCache.put(fastKey, renderPage(pdfRenderer, pageIndex, FAST_WIDTH).first)
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                        }
                    }
                }
            }
        }
    }

    var showGoToPage by remember { mutableStateOf(false) }

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
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(
                start = WeBeepSpacing.xs,
                end = WeBeepSpacing.xs,
                top = contentPadding.calculateTopPadding() + WeBeepSpacing.xs,
                bottom = contentPadding.calculateBottomPadding() + PageIndicatorClearance
            ),
            verticalArrangement = Arrangement.spacedBy(WeBeepSpacing.xs),
            modifier = Modifier
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
                    pdfRenderer = pdfRenderer,
                    pageIndex = pageIndex,
                    defaultAspectRatio = defaultAspectRatio,
                    highResWidth = highResWidth,
                    memoryCache = memoryCache,
                    pdfMutex = pdfMutex,
                    ioDispatcher = ioDispatcher
                )
            }
        }

        // Always visible (dimmed at rest) so it can be tapped to jump to a page.
        val indicatorAlpha by animateFloatAsState(
            targetValue = if (lazyListState.isScrollInProgress) 1f else IdleIndicatorAlpha,
            animationSpec = WeBeepMotion.smooth(reduceMotion = reduceMotion),
            label = "page_indicator_alpha"
        )
        Surface(
            shape = CircleShape,
            color = WeBeepTheme.colors.mediaScrim.copy(alpha = 0.65f),
            contentColor = WeBeepTheme.colors.onMedia,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .navigationBarsPadding()
                .padding(WeBeepSpacing.md)
                .alpha(indicatorAlpha)
                .clip(CircleShape)
                .clickable(onClickLabel = stringResource(R.string.file_viewer_go_to_page)) { showGoToPage = true }
        ) {
            Text(
                text = "${currentPage + 1} / $pageCount",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(horizontal = WeBeepSpacing.sm, vertical = WeBeepSpacing.xs)
            )
        }
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
private fun GoToPageDialog(
    pageCount: Int,
    onDismiss: () -> Unit,
    onGo: (Int) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val page = text.toIntOrNull()?.takeIf { it in 1..pageCount }
    val submit = { page?.let { onGo(it - 1) } }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.file_viewer_go_to_page)) },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { value -> text = value.filter { it.isDigit() }.take(5) },
                label = { Text(stringResource(R.string.file_viewer_page_hint, pageCount)) },
                singleLine = true,
                isError = text.isNotEmpty() && page == null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = { submit() })
            )
        },
        confirmButton = {
            TextButton(onClick = { submit() }, enabled = page != null) {
                Text(stringResource(R.string.file_viewer_go))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}

/** Renders [pageIndex] at [width] px on white; returns the bitmap and the page aspect ratio. */
private fun renderPage(renderer: PdfRenderer, pageIndex: Int, width: Int): Pair<Bitmap, Float> {
    val page = renderer.openPage(pageIndex)
    try {
        val ratio = page.width.toFloat() / page.height.toFloat()
        val height = (width / ratio).roundToInt().coerceAtLeast(1)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        return bitmap to ratio
    } finally {
        page.close()
    }
}

private val PageIndicatorClearance = 72.dp
private val PageCornerRadius = 4.dp
private val PageElevation = 2.dp
private const val IdleIndicatorAlpha = 0.7f

private class PdfSession(
    val descriptor: ParcelFileDescriptor,
    val renderer: PdfRenderer
) {
    fun close() {
        try {
            renderer.close()
        } catch (_: Exception) {}
        try {
            descriptor.close()
        } catch (_: Exception) {}
    }
}

private class OpenedPdf(val session: PdfSession, val firstPageAspectRatio: Float)

private sealed interface PdfLoadState {
    data object Loading : PdfLoadState
    class Ready(val session: PdfSession, val defaultAspectRatio: Float) : PdfLoadState
    data object Error : PdfLoadState
}

/**
 * Opens the renderer and reads the first-page ratio (placeholder that prevents
 * scroll jumps). Blocking: call on an I/O dispatcher while holding the PDF mutex.
 */
private fun openPdfSession(file: File): OpenedPdf? {
    var descriptor: ParcelFileDescriptor? = null
    return try {
        val opened = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        descriptor = opened
        val renderer = PdfRenderer(opened)
        val ratio = try {
            renderer.openPage(0).use { page -> page.width.toFloat() / page.height.toFloat() }
        } catch (_: Exception) {
            DEFAULT_ASPECT_RATIO
        }
        OpenedPdf(PdfSession(opened, renderer), ratio)
    } catch (_: Exception) {
        try {
            descriptor?.close()
        } catch (_: Exception) {}
        null
    }
}

private const val MAX_HIGH_RES_WIDTH = 1800
private const val FAST_WIDTH = 800
private const val DEFAULT_ASPECT_RATIO = 1f / 1.414f

private const val MIN_CACHE_SIZE_KB = 8 * 1024
private const val MAX_CACHE_SIZE_KB = 24 * 1024

data class PageRenderResult(
    val fastBitmap: Bitmap? = null,
    val highResBitmap: Bitmap? = null,
    val aspectRatio: Float
)

@Composable
private fun PdfPageItem(
    pdfRenderer: PdfRenderer,
    pageIndex: Int,
    defaultAspectRatio: Float,
    highResWidth: Int,
    memoryCache: android.util.LruCache<String, Bitmap>,
    pdfMutex: Mutex,
    ioDispatcher: CoroutineDispatcher
) {
    val fastKey = "fast_$pageIndex"
    val highKey = "high_$pageIndex"

    val pageResult by produceState(
        initialValue = PageRenderResult(
            fastBitmap = memoryCache.get(fastKey),
            highResBitmap = memoryCache.get(highKey),
            aspectRatio = defaultAspectRatio
        ), 
        pdfRenderer, pageIndex, highResWidth
    ) {
        // Skip rendering entirely when HD is cached: saves CPU/battery.
        if (value.highResBitmap != null) {
            return@produceState
        }

        withContext(ioDispatcher) {
            kotlinx.coroutines.delay(100)
            if (!isActive) return@withContext

            val cachedFast: Bitmap? = memoryCache.get(fastKey)
            var fastBitmap: Bitmap? = cachedFast
            var currentAspectRatio = cachedFast?.let { it.width.toFloat() / it.height.toFloat() } ?: defaultAspectRatio

            if (fastBitmap == null) {
                pdfMutex.withLock {
                    if (!isActive) return@withContext
                    try {
                        val (rendered, ratio) = renderPage(pdfRenderer, pageIndex, FAST_WIDTH)
                        currentAspectRatio = ratio
                        fastBitmap = rendered
                        memoryCache.put(fastKey, rendered)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
            }

            if (fastBitmap == null) return@withContext

            // Show the fast bitmap instantly, upgrade to HD once scrolling settles.
            value = PageRenderResult(fastBitmap = fastBitmap, aspectRatio = currentAspectRatio)

            // Screens narrower than the fast render gain nothing from an HD pass.
            if (highResWidth <= FAST_WIDTH) return@withContext

            kotlinx.coroutines.delay(250)
            if (!isActive) return@withContext

            var highResBitmap: Bitmap? = memoryCache.get(highKey)
            if (highResBitmap == null) {
                pdfMutex.withLock {
                    if (!isActive) return@withContext
                    try {
                        val (rendered, _) = renderPage(pdfRenderer, pageIndex, highResWidth)
                        highResBitmap = rendered
                        memoryCache.put(highKey, rendered)
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                    }
                }
            }

            if (highResBitmap != null) {
                value = PageRenderResult(fastBitmap = fastBitmap, highResBitmap = highResBitmap, aspectRatio = currentAspectRatio)
            }
        }
    }

    val reduceMotion = LocalReduceMotion.current
    val pageShape = RoundedCornerShape(PageCornerRadius)
    val pageDescription = stringResource(R.string.cd_pdf_page, pageIndex + 1)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(pageResult.aspectRatio)
            .shadow(PageElevation, pageShape)
            .background(androidx.compose.ui.graphics.Color.White, pageShape)
    ) {
        // Blank page -> fast render -> HD: each step fades in instead of popping.
        Crossfade(
            targetState = pageResult.highResBitmap ?: pageResult.fastBitmap,
            animationSpec = WeBeepMotion.smooth(durationMs = WeBeepMotion.ShortMs, reduceMotion = reduceMotion),
            label = "pdf_page_bitmap"
        ) { bitmap ->
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = pageDescription,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillWidth
                )
            } else {
                Box(Modifier.fillMaxSize())
            }
        }
    }
}
