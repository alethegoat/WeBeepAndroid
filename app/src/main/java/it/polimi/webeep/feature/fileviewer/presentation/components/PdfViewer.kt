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
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import kotlinx.coroutines.launch
import androidx.compose.ui.geometry.Offset
import it.polimi.webeep.core.ui.util.ScrollBoundaryHaptics
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.roundToInt

/**
 * component for viewing pdf files with pinch-to-zoom and lazy loading.
 */
@Composable
fun PdfViewer(
    file: File,
    pageCount: Int,
    modifier: Modifier = Modifier,
    ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    var scale by remember { mutableStateOf(1f) }
    var offsetX by remember { mutableStateOf(0f) }
    var offsetY by remember { mutableStateOf(0f) }
    
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current

    val pdfRenderer = remember(file) {
        val descriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        PdfRenderer(descriptor)
    }

    // calculate the aspect ratio of the first page as default to prevent visual jumps during scroll
    val defaultAspectRatio = remember(pdfRenderer) {
        try {
            val page = pdfRenderer.openPage(0)
            val ratio = page.width.toFloat() / page.height.toFloat()
            page.close()
            ratio
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            1f / 1.414f // fallback a4
        }
    }

    val pdfMutex = remember { Mutex() }
    val fastBitmapPool = remember { ConcurrentLinkedQueue<Bitmap>() }
    val highResBitmapPool = remember { ConcurrentLinkedQueue<Bitmap>() }

    // implement an lrucache to zero out cpu and battery consumption when re-viewing already read pages.
    // use 1/6 of available heap memory, usually around 40-60mb (enough for 4-8 hd pages or 15-20 fast pages).
    val memoryCache = remember {
        val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
        val cacheSize = maxMemory / 6
        object : android.util.LruCache<String, Bitmap>(cacheSize) {
            override fun sizeOf(key: String, bitmap: Bitmap): Int {
                return bitmap.byteCount / 1024 // measure cache in kilobytes
            }

            override fun entryRemoved(evicted: Boolean, key: String, oldValue: Bitmap, newValue: Bitmap?) {
                if (evicted) {
                    if (key.startsWith("fast_")) {
                        fastBitmapPool.add(oldValue)
                    } else if (key.startsWith("high_")) {
                        highResBitmapPool.add(oldValue)
                    }
                }
            }
        }
    }

    DisposableEffect(pdfRenderer) {
        onDispose {
            memoryCache.evictAll()
            try {
                pdfRenderer.close()
            } catch (_: Exception) {}
        }
    }

    // scroll state and page tracking
    val lazyListState = rememberLazyListState()
    
    // activate scroll haptic feedback at the top/bottom boundaries of the pdf
    ScrollBoundaryHaptics(state = lazyListState)

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
            // stay visible for 1500ms after scrolling stops
            kotlinx.coroutines.delay(1500)
            isIndicatorVisible = false
        }
    }

    // silent pre-fetching: preload the next and previous pages
    LaunchedEffect(currentPage) {
        val pagesToFetch = listOf(currentPage + 1, currentPage - 1).filter { it in 0 until pageCount }
        pagesToFetch.forEach { pageIndex ->
            launch(ioDispatcher) {
                val fastKey = "fast_$pageIndex"
                if (memoryCache.get(fastKey) == null) {
                    pdfMutex.withLock {
                        if (memoryCache.get(fastKey) != null || !isActive) return@withLock
                        var pooledBitmap: Bitmap? = null
                        try {
                            val page = pdfRenderer.openPage(pageIndex)
                            try {
                                val FAST_WIDTH = 800
                                val renderScale = FAST_WIDTH.toFloat() / page.width.toFloat()
                                val height = (page.height * renderScale).roundToInt().coerceAtLeast(1)
                                
                                pooledBitmap = fastBitmapPool.poll()
                                while (pooledBitmap != null && (pooledBitmap.width != FAST_WIDTH || pooledBitmap.height != height)) {
                                    pooledBitmap.recycle()
                                    pooledBitmap = fastBitmapPool.poll()
                                }
                                
                                val bmp = pooledBitmap ?: Bitmap.createBitmap(FAST_WIDTH, height, Bitmap.Config.ARGB_8888)
                                bmp.eraseColor(Color.WHITE)
                                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                                memoryCache.put(fastKey, bmp)
                            } finally {
                                page.close()
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                            pooledBitmap?.recycle()
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
                    onDoubleTap = { tapOffset ->
                        val targetScale = if (scale > 1f) 1f else 3f
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val relativeTap = tapOffset - center
                        val maxX = (size.width * (targetScale - 1f)) / 2f
                        val maxY = (size.height * (targetScale - 1f)) / 2f
                        val targetOffsetX = if (targetScale == 1f) 0f else (-relativeTap.x * (targetScale - 1f)).coerceIn(-maxX, maxX)
                        val targetOffsetY = if (targetScale == 1f) 0f else (-relativeTap.y * (targetScale - 1f)).coerceIn(-maxY, maxY)

                        try {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                            } else {
                                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                            }
                        } catch (e: Exception) {
                            if (e is CancellationException) throw e
                        }

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
                // custom pinch-to-zoom management: intercepts only multi-touch gestures (2+ fingers),
                // letting single-finger drags pass to the lazycolumn for fluid scrolling.
                .pointerInput(Unit) {
                    awaitEachGesture {
                        // wait for the first touch
                        awaitFirstDown(requireUnconsumed = false)

                        do {
                            val event = awaitPointerEvent()
                            val pointers = event.changes.filter { it.pressed }

                            if (pointers.size >= 2) {
                                // multi-touch: handle zoom and pan
                                val zoom = event.calculateZoom()
                                val pan = event.calculatePan()

                                val oldScale = scale
                                scale = (scale * zoom).coerceIn(1f, 5f)

                                if ((scale == 1f || scale == 5f) && oldScale != scale) {
                                    try {
                                        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                                            view.performHapticFeedback(HapticFeedbackConstants.GESTURE_END)
                                        } else {
                                            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
                                        }
                                    } catch (e: Exception) {
                                        if (e is CancellationException) throw e
                                    }
                                }

                                if (scale > 1f) {
                                    val maxX = (size.width * (scale - 1)) / 2
                                    val maxY = (size.height * (scale - 1)) / 2
                                    offsetX = (offsetX + pan.x).coerceIn(-maxX.toFloat(), maxX.toFloat())
                                    offsetY = (offsetY + pan.y).coerceIn(-maxY.toFloat(), maxY.toFloat())
                                } else {
                                    offsetX = 0f
                                    offsetY = 0f
                                }

                                // consume pointers to avoid conflicts with the lazycolumn
                                event.changes.forEach { change ->
                                    if (change.positionChanged()) {
                                        change.consume()
                                    }
                                }
                            } else if (pointers.size == 1 && scale > 1f) {
                                // one finger + active zoom: pan on the zoomed view
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
                            // one finger + scale == 1f: do not consume anything -> the lazycolumn handles scrolling
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
                    memoryCache = memoryCache,
                    pdfMutex = pdfMutex,
                    fastBitmapPool = fastBitmapPool,
                    highResBitmapPool = highResBitmapPool,
                    ioDispatcher = ioDispatcher
                )
            }
        }

        // floating page indicator overlay
        AnimatedVisibility(
            visible = isIndicatorVisible,
            enter = fadeIn(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(500)),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = androidx.compose.ui.graphics.Color.Black.copy(alpha = 0.65f),
                contentColor = androidx.compose.ui.graphics.Color.White
            ) {
                Text(
                    text = "${currentPage + 1} / $pageCount",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }
    }
}

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
    memoryCache: android.util.LruCache<String, Bitmap>,
    pdfMutex: Mutex,
    fastBitmapPool: ConcurrentLinkedQueue<Bitmap>,
    highResBitmapPool: ConcurrentLinkedQueue<Bitmap>,
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
        pdfRenderer, pageIndex
    ) {
        // if the hd version is already in ram, we don't need to consume cpu or battery.
        if (value.highResBitmap != null) {
            return@produceState
        }

        withContext(ioDispatcher) {
            // debounce for extremely fast scrolling
            kotlinx.coroutines.delay(100)
            if (!isActive) return@withContext

            var fastBitmap: Bitmap? = memoryCache.get(fastKey)
            var currentAspectRatio = defaultAspectRatio

            // 1. render fast bitmap if not in cache
            if (fastBitmap == null) {
                pdfMutex.withLock {
                    if (!isActive) return@withContext
                    var pooledBitmap: Bitmap? = null
                    try {
                        val page = pdfRenderer.openPage(pageIndex)
                        try {
                            currentAspectRatio = page.width.toFloat() / page.height.toFloat()
                            val FAST_WIDTH = 800
                            val renderScale = FAST_WIDTH.toFloat() / page.width.toFloat()
                            val height = (page.height * renderScale).roundToInt().coerceAtLeast(1)
                            
                            pooledBitmap = fastBitmapPool.poll()
                            while (pooledBitmap != null && (pooledBitmap.width != FAST_WIDTH || pooledBitmap.height != height)) {
                                pooledBitmap.recycle()
                                pooledBitmap = fastBitmapPool.poll()
                            }
                            
                            fastBitmap = pooledBitmap ?: Bitmap.createBitmap(FAST_WIDTH, height, Bitmap.Config.ARGB_8888)
                            fastBitmap!!.eraseColor(Color.WHITE)
                            page.render(fastBitmap!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            memoryCache.put(fastKey, fastBitmap)
                        } finally {
                            page.close()
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        pooledBitmap?.recycle()
                    }
                }
            }

            if (fastBitmap == null) return@withContext

            // emit the fastbitmap to show it instantly
            value = PageRenderResult(fastBitmap = fastBitmap, aspectRatio = currentAspectRatio)

            // 2. wait before loading the hd version
            kotlinx.coroutines.delay(250)
            if (!isActive) return@withContext

            // 3. render high res bitmap if not in cache
            var highResBitmap: Bitmap? = memoryCache.get(highKey)
            if (highResBitmap == null) {
                pdfMutex.withLock {
                    if (!isActive) return@withContext
                    var pooledBitmap: Bitmap? = null
                    try {
                        val page = pdfRenderer.openPage(pageIndex)
                        try {
                            val HIGH_RES_WIDTH = 1800
                            val renderScale = HIGH_RES_WIDTH.toFloat() / page.width.toFloat()
                            val height = (page.height * renderScale).roundToInt().coerceAtLeast(1)
                            
                            pooledBitmap = highResBitmapPool.poll()
                            while (pooledBitmap != null && (pooledBitmap.width != HIGH_RES_WIDTH || pooledBitmap.height != height)) {
                                pooledBitmap.recycle()
                                pooledBitmap = highResBitmapPool.poll()
                            }
                            
                            highResBitmap = pooledBitmap ?: Bitmap.createBitmap(HIGH_RES_WIDTH, height, Bitmap.Config.ARGB_8888)
                            highResBitmap!!.eraseColor(Color.WHITE)
                            page.render(highResBitmap!!, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            memoryCache.put(highKey, highResBitmap)
                        } finally {
                            page.close()
                        }
                    } catch (e: Exception) {
                        if (e is CancellationException) throw e
                        pooledBitmap?.recycle()
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
            val bitmapToDraw = pageResult.highResBitmap ?: pageResult.fastBitmap!!
            Image(
                bitmap = bitmapToDraw.asImageBitmap(),
                contentDescription = "PDF Page $pageIndex",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth
            )
        }
    }
}
