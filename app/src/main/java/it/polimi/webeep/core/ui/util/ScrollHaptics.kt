package it.polimi.webeep.core.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable

/**
 * Scroll-boundary haptics intentionally disabled to avoid vibration noise.
 */
@Composable
fun ScrollBoundaryHaptics(
    state: LazyListState
) {
}

@Composable
fun ScrollBoundaryHaptics(
    state: LazyGridState
) {
}
