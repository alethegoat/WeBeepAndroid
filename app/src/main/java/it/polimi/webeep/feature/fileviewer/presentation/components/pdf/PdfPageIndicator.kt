package it.polimi.webeep.feature.fileviewer.presentation.components.pdf

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import it.polimi.webeep.R
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.theme.WeBeepSpacing
import it.polimi.webeep.core.ui.theme.WeBeepTheme
import it.polimi.webeep.core.ui.util.LocalReduceMotion

/** Bottom list padding that keeps the last page clear of the floating [PdfPageIndicator]. */
internal val PageIndicatorClearance = 72.dp

private const val IDLE_INDICATOR_ALPHA = 0.7f
private const val INDICATOR_SCRIM_ALPHA = 0.65f

/** "3 / 12" pill. Always visible (dimmed at rest) so it can be tapped to jump to a page. */
@Composable
internal fun PdfPageIndicator(currentPage: Int, pageCount: Int, isScrolling: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val indicatorAlpha by animateFloatAsState(
        targetValue = if (isScrolling) 1f else IDLE_INDICATOR_ALPHA,
        animationSpec = WeBeepMotion.smooth(reduceMotion = LocalReduceMotion.current),
        label = "page_indicator_alpha"
    )
    Surface(
        shape = CircleShape,
        color = WeBeepTheme.colors.mediaScrim.copy(alpha = INDICATOR_SCRIM_ALPHA),
        contentColor = WeBeepTheme.colors.onMedia,
        modifier = modifier
            .navigationBarsPadding()
            .padding(WeBeepSpacing.md)
            .alpha(indicatorAlpha)
            .clip(CircleShape)
            .clickable(onClickLabel = stringResource(R.string.file_viewer_go_to_page), onClick = onClick)
    ) {
        Text(
            text = "${currentPage + 1} / $pageCount",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            modifier = Modifier.padding(horizontal = WeBeepSpacing.sm, vertical = WeBeepSpacing.xs)
        )
    }
}
