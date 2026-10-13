package it.polimi.webeep.core.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Material 3 window width classes. */
enum class WindowWidthClass { COMPACT, MEDIUM, EXPANDED }

/** Size of the app window, read from the window itself (split screen and freeform included). */
@Immutable
data class WindowSize(val width: Dp, val height: Dp) {
    val widthClass: WindowWidthClass
        get() = when {
            width < MediumWidth -> WindowWidthClass.COMPACT
            width < ExpandedWidth -> WindowWidthClass.MEDIUM
            else -> WindowWidthClass.EXPANDED
        }

    /** Wider than tall and wide enough for two panes side by side (landscape phones, tablets). */
    val isWideLandscape: Boolean
        get() = width >= MediumWidth && width > height

    private companion object {
        val MediumWidth = 600.dp
        val ExpandedWidth = 840.dp
    }
}

/** Current window size; recomposes when the window is resized or rotated. */
@Composable
fun currentWindowSize(): WindowSize {
    val size = LocalWindowInfo.current.containerSize
    return with(LocalDensity.current) { WindowSize(size.width.toDp(), size.height.toDp()) }
}
