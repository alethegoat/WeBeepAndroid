package it.polimi.webeep.feature.fileviewer.presentation.components.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.LruCache
import it.polimi.webeep.core.common.runCatchingCancellable
import java.io.File
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/** A rendered page and the aspect ratio (width / height) it was rendered with. */
class PdfPageRender(val bitmap: Bitmap, val aspectRatio: Float)

/**
 * One open PDF: the [PdfRenderer], its page bitmap cache and the mutex that serialises
 * every access to the renderer (it supports a single open page at a time). All suspend
 * functions run on [ioDispatcher].
 *
 * The cache is small and hard-capped. Evicted bitmaps are never recycled: a composed
 * Image may still be drawing them.
 */
class PdfRendererSession(private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO, cacheSizeKb: Int = defaultCacheSizeKb()) {
    private val mutex = Mutex()
    private var descriptor: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null

    private val cache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount / 1024
    }

    /**
     * Opens [file] and returns the first-page aspect ratio (the placeholder that prevents
     * scroll jumps), or null if it cannot be opened. The renderer is stored before returning
     * so a cancellation right after still lets [close] release it.
     */
    suspend fun open(file: File): Float? = withContext(ioDispatcher) {
        mutex.withLock { openBlocking(file) }
    }

    fun cachedFast(pageIndex: Int): Bitmap? = cache.get(fastKey(pageIndex))

    fun cachedHighRes(pageIndex: Int): Bitmap? = cache.get(highKey(pageIndex))

    /** Low-resolution render of [pageIndex] (cached), or null if it failed or was cancelled. */
    suspend fun fastPage(pageIndex: Int): PdfPageRender? = withContext(ioDispatcher) {
        cachedFast(pageIndex)?.let { return@withContext PdfPageRender(it, it.width.toFloat() / it.height.toFloat()) }
        mutex.withLock {
            currentCoroutineContext().ensureActive()
            runCatchingCancellable {
                renderPage(pageIndex, FAST_WIDTH).also { cache.put(fastKey(pageIndex), it.bitmap) }
            }.getOrNull()
        }
    }

    /** Render of [pageIndex] at [width] px (cached), or null if it failed or was cancelled. */
    suspend fun highResPage(pageIndex: Int, width: Int): Bitmap? = withContext(ioDispatcher) {
        cachedHighRes(pageIndex)?.let { return@withContext it }
        mutex.withLock {
            currentCoroutineContext().ensureActive()
            runCatchingCancellable {
                renderPage(pageIndex, width).bitmap.also { cache.put(highKey(pageIndex), it) }
            }.getOrNull()
        }
    }

    /** Warms the cache with the low-resolution neighbours of [page]. */
    suspend fun prefetchAround(page: Int, pageCount: Int) = coroutineScope {
        listOf(page + 1, page - 1).filter { it in 0 until pageCount }.forEach { pageIndex ->
            launch(ioDispatcher) { prefetchFast(pageIndex) }
        }
    }

    /** Frees the cache, the renderer and the file. Safe to call more than once, even when cancelled. */
    suspend fun close() {
        withContext(NonCancellable + ioDispatcher) {
            mutex.withLock {
                cache.evictAll()
                closeQuietly { renderer?.close() }
                closeQuietly { descriptor?.close() }
                renderer = null
                descriptor = null
            }
        }
    }

    private suspend fun prefetchFast(pageIndex: Int) {
        if (cachedFast(pageIndex) != null) return
        mutex.withLock {
            if (cachedFast(pageIndex) != null) return
            currentCoroutineContext().ensureActive()
            // Prefetch is best effort: a page that fails to render is simply left uncached.
            runCatchingCancellable { cache.put(fastKey(pageIndex), renderPage(pageIndex, FAST_WIDTH).bitmap) }
        }
    }

    /** Blocking: call on an I/O dispatcher while holding [mutex]. */
    private fun openBlocking(file: File): Float? {
        var opened: ParcelFileDescriptor? = null
        return try {
            opened = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            descriptor = opened
            val pdf = PdfRenderer(opened)
            renderer = pdf
            try {
                pdf.openPage(0).use { page -> page.width.toFloat() / page.height.toFloat() }
            } catch (_: Exception) {
                DEFAULT_ASPECT_RATIO
            }
        } catch (_: Exception) {
            closeQuietly { opened?.close() }
            descriptor = null
            null
        }
    }

    /** Renders [pageIndex] at [width] px on white. Blocking: call while holding [mutex]. */
    private fun renderPage(pageIndex: Int, width: Int): PdfPageRender {
        val pdf = checkNotNull(renderer) { "PDF is not open" }
        val page = pdf.openPage(pageIndex)
        try {
            val ratio = page.width.toFloat() / page.height.toFloat()
            val height = (width / ratio).roundToInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return PdfPageRender(bitmap, ratio)
        } finally {
            page.close()
        }
    }

    private inline fun closeQuietly(block: () -> Unit) {
        try {
            block()
        } catch (_: Exception) {
        }
    }

    private fun fastKey(pageIndex: Int) = "fast_$pageIndex"

    private fun highKey(pageIndex: Int) = "high_$pageIndex"

    companion object {
        /** Width of the quick first render, shown while the HD one is still pending. */
        const val FAST_WIDTH = 800

        /** A4 portrait, used when the first page cannot be measured. */
        const val DEFAULT_ASPECT_RATIO = 1f / 1.414f

        private const val BYTES_PER_KB = 1024
        private const val HEAP_DIVISOR = 10
        private const val MIN_CACHE_SIZE_KB = 8 * BYTES_PER_KB
        private const val MAX_CACHE_SIZE_KB = 24 * BYTES_PER_KB

        /** A tenth of the heap, clamped to a small range. */
        fun defaultCacheSizeKb(): Int {
            val maxMemoryKb = (Runtime.getRuntime().maxMemory() / BYTES_PER_KB).toInt()
            return (maxMemoryKb / HEAP_DIVISOR).coerceAtLeast(MIN_CACHE_SIZE_KB).coerceAtMost(MAX_CACHE_SIZE_KB)
        }
    }
}
