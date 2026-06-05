package it.polimi.webeep.core.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable

/**
 * adds a haptic feedback when reaching the upper or lower scroll limit
 * of a lazycolumn / lazyrow. (disabled to reduce intrusiveness).
 */
@Composable
fun ScrollBoundaryHaptics(
    state: LazyListState
) {
    // intentionally empty. removed to avoid saturating the user with vibrations.
}

/**
 * adds a haptic feedback when reaching the upper or lower scroll limit
 * of a lazyverticalgrid / lazyhorizontalgrid. (disabled to reduce intrusiveness).
 */
@Composable
fun ScrollBoundaryHaptics(
    state: LazyGridState
) {
    // intentionally empty. removed to avoid saturating the user with vibrations.
}
