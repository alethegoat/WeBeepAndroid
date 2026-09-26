package it.polimi.webeep.feature.fileviewer.presentation.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
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
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    onToggleFullScreen: () -> Unit = {}
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
            modifier = modifier
        )
    }
}

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
    modifier: Modifier = Modifier
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }

    val coroutineScope = rememberCoroutineScope()

    val lazyListState = rememberLazyListState()

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

    var isIndicatorVisible by remember { mutableStateOf(false) }
    LaunchedEffect(lazyListState.isScrollInProgress, currentPage) {
        if (lazyListState.isScrollInProgress) {
            isIndicatorVisible = true
        } else {
            kotlinx.coroutines.delay(1500)
            isIndicatorVisible = false
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
                            val page = pdfRenderer.openPage(pageIndex)
                            try {
                                val renderScale = FAST_WIDTH.toFloat() / page.width.toFloat()
                                val height = (page.height * renderScale).roundToInt().coerceAtLeast(1)
                                val bmp = Bitmap.createBitmap(FAST_WIDTH, height, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                memoryCache.put(fastKey, bmp)
                            } finally {
                                page.close()
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                        }
                    }
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onToggleFullScreen() },
                    onDoubleTap = { tapOffset ->
                        val targetScale = if (scale > 1f) 1f else 3f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val relativeTap = tapOffset - center
                        val maxX = (size.width * (targetScale - 1f)) / 2f
                        val maxY = (size.height * (targetScale - 1f)) / 2f
                        val targetOffsetX = if (targetScale == 1f) 0f else (-relativeTap.x * (targetScale - 1f)).coerceIn(-maxX, maxX)
                        val targetOffsetY = if (targetScale == 1f) 0f else (-relativeTap.y * (targetScale - 1f)).coerceIn(-maxY, maxY)

                        coroutineScope.launch {
                            val startScale = scale
                            val startOffsetX = offsetX
                            val startOffsetY = offsetY

                            animate(
                                initialValue = 0f,
                                targetValue = 1f,
                                animationSpec = tween(durationMillis = 300)
                            ) { progress, _ ->
                                scale = startScale + (targetScale - startScale) * progress
                                offsetX = startOffsetX + (targetOffsetX - startOffsetX) * progress
                                offsetY = startOffsetY + (targetOffsetY - startOffsetY) * progress
                            }
                        }
                    }
                )
            }
    ) {
        LazyColumn(
            state = lazyListState,
            userScrollEnabled = scale <= 1f,
            modifier = Modifier
                .fillMaxSize()
                // Multi-touch zooms/pans; single-finger drags fall through to the LazyColumn.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false)

                        do {
                            val event = awaitPointerEvent()
                            val pointers = event.changes.filter { it.pressed }

                            if (pointers.size >= 2) {
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()

                                scale = (scale * zoom).coerceIn(1f, 5f)

                                if (scale > 1f) {
                                    val maxX = (size.width * (scale - 1)) / 2
                                    val maxY = (size.height * (scale - 1)) / 2
                                    offsetX = (offsetX + pan.x).coerceIn(-maxX.toFloat(), maxX.toFloat())
                                    offsetY = (offsetY + pan.y).coerceIn(-maxY.toFloat(), maxY.toFloat())
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }

                                // Consume so the LazyColumn doesn't steal the gesture.
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) {
                                        change.consume()
                                    }
                                }
                            } else if (pointers.size == 1 && scale > 1f) {
                                // Pan the zoomed page with one finger.
                                val pan = event.calculatePan()
                                val maxX = (size.width * (scale - 1)) / 2
                                val maxY = (size.height * (scale - 1)) / 2
                                offsetX = (offsetX + pan.x).coerceIn(-maxX.toFloat(), maxX.toFloat())
                                offsetY = (offsetY + pan.y).coerceIn(-maxY.toFloat(), maxY.toFloat())

                                event.changes.forEach { change ->
                                    if (change.positionChanged()) {
                                        change.consume()
                                    }
                                }
                            }
                            // At scale 1f consume nothing: the LazyColumn scrolls.
                        } while (event.changes.any { it.pressed })
                    }
                }
                .graphicsLayer(
                    scaleX = scale,
                    scaleY = scale,
                    translationX = offsetX,
                    translationY = offsetY
                )
        ) {
            itemsIndexed(
                items = List(pageCount) { it },
                key = { _, index -> index },
                contentType = { _, _ -> "pdf_page" }
            ) { _, pageIndex ->
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

        AnimatedVisibility(
            visible = isIndicatorVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(WeBeepSpacing.md)
        ) {
            Surface(
                shape = CircleShape,
                color = WeBeepTheme.colors.mediaScrim.copy(alpha = 0.65f),
                contentColor = WeBeepTheme.colors.onMedia
            ) {
                Text(
                    text = "${currentPage + 1} / $pageCount",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = WeBeepSpacing.sm, vertical = WeBeepSpacing.xxs)
                )
            }
        }
    }
}

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

            var fastBitmap: Bitmap? = memoryCache.get(fastKey)
            var currentAspectRatio = defaultAspectRatio

            if (fastBitmap == null) {
                pdfMutex.withLock {
                    if (!isActive) return@withContext
                    try {
                        val page = pdfRenderer.openPage(pageIndex)
                        try {
                            currentAspectRatio = page.width.toFloat() / page.height.toFloat()
                            val renderScale = FAST_WIDTH.toFloat() / page.width.toFloat()
                            val height = (page.height * renderScale).roundToInt().coerceAtLeast(1)
                            val rendered = Bitmap.createBitmap(FAST_WIDTH, height, Bitmap.Config.ARGB_8888)
                            rendered.eraseColor(Color.WHITE)
                            page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            fastBitmap = rendered
                            memoryCache.put(fastKey, rendered)
                        } finally {
                            page.close()
                        }
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
                        val page = pdfRenderer.openPage(pageIndex)
                        try {
                            val renderScale = highResWidth.toFloat() / page.width.toFloat()
                            val height = (page.height * renderScale).roundToInt().coerceAtLeast(1)
                            val rendered = Bitmap.createBitmap(highResWidth, height, Bitmap.Config.ARGB_8888)
                            rendered.eraseColor(Color.WHITE)
                            page.render(rendered, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            highResBitmap = rendered
                            memoryCache.put(highKey, rendered)
                        } finally {
                            page.close()
                        }
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

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(pageResult.aspectRatio),
        contentAlignment = Alignment.Center
    ) {
        if (pageResult.fastBitmap == null) {
            CircularProgressIndicator()
        } else {
            val bitmapToDraw = pageResult.highResBitmap ?: pageResult.fastBitmap
            if (bitmapToDraw != null) {
            Image(
                bitmap = bitmapToDraw.asImageBitmap(),
                contentDescription = androidx.compose.ui.res.stringResource(it.polimi.webeep.R.string.cd_pdf_page, pageIndex + 1),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth
            )
            }
        }
    }
}
