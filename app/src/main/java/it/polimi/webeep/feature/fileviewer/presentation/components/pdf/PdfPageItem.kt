package it.polimi.webeep.feature.fileviewer.presentation.components.pdf

import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.util.LocalReduceMotion
import kotlinx.coroutines.delay

/** Readable page width on tablets and landscape; phones stay edge to edge. */
internal val MaxPageWidth = 900.dp

private val PageCornerRadius = 4.dp
private val PageElevation = 2.dp

/** Lets a fast fling past the page before spending CPU on rendering it. */
private const val FAST_RENDER_DELAY_MS = 100L

/** Waits for scrolling to settle before upgrading a page to HD. */
private const val HIGH_RES_RENDER_DELAY_MS = 250L

@Immutable
private data class PageRenderResult(val fastBitmap: Bitmap? = null, val highResBitmap: Bitmap? = null, val aspectRatio: Float)

@Composable
internal fun PdfPageItem(
    session: PdfRendererSession,
    pageIndex: Int,
    defaultAspectRatio: Float,
    highResWidth: Int,
    modifier: Modifier = Modifier
) {
    val pageResult by rememberPageRender(session, pageIndex, defaultAspectRatio, highResWidth)
    val reduceMotion = LocalReduceMotion.current
    val pageShape = RoundedCornerShape(PageCornerRadius)
    val pageDescription = stringResource(R.string.cd_pdf_page, pageIndex + 1)
    Box(
        modifier = modifier
            .widthIn(max = MaxPageWidth)
            .fillMaxWidth()
            .aspectRatio(pageResult.aspectRatio)
            .shadow(PageElevation, pageShape)
            .background(Color.White, pageShape)
    ) {
        // Blank page -> fast render -> HD: each step fades in instead of popping.
        Crossfade(
            targetState = pageResult.highResBitmap ?: pageResult.fastBitmap,
            animationSpec = WeBeepMotion.smooth(durationMs = WeBeepMotion.SHORT_MS, reduceMotion = reduceMotion),
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

/** Starts from whatever is cached, then renders the fast and finally the HD bitmap. */
@Composable
private fun rememberPageRender(
    session: PdfRendererSession,
    pageIndex: Int,
    defaultAspectRatio: Float,
    highResWidth: Int
): State<PageRenderResult> = produceState(
    initialValue = PageRenderResult(
        fastBitmap = session.cachedFast(pageIndex),
        highResBitmap = session.cachedHighRes(pageIndex),
        aspectRatio = defaultAspectRatio
    ),
    session,
    pageIndex,
    highResWidth
) {
    // Skip rendering entirely when HD is cached: saves CPU/battery.
    if (value.highResBitmap != null) return@produceState

    delay(FAST_RENDER_DELAY_MS)
    val fast = session.fastPage(pageIndex) ?: return@produceState

    // Show the fast bitmap instantly, upgrade to HD once scrolling settles.
    value = PageRenderResult(fastBitmap = fast.bitmap, aspectRatio = fast.aspectRatio)

    // Screens narrower than the fast render gain nothing from an HD pass.
    if (highResWidth <= PdfRendererSession.FAST_WIDTH) return@produceState

    delay(HIGH_RES_RENDER_DELAY_MS)
    val highRes = session.highResPage(pageIndex, highResWidth) ?: return@produceState
    value = PageRenderResult(fastBitmap = fast.bitmap, highResBitmap = highRes, aspectRatio = fast.aspectRatio)
}
