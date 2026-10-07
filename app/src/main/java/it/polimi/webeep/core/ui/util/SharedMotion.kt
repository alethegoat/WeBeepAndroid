@file:OptIn(ExperimentalSharedTransitionApi::class)

package it.polimi.webeep.core.ui.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.theme.WeBeepMotion

val LocalNavSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

private val CourseContainerShape = RoundedCornerShape(24.dp)

/**
 * Container-transform between dashboard [CourseCard] and the whole course-detail
 * screen. Content is remeasured to the animating bounds and clipped to the card
 * shape, so card corners morph instead of popping. Apply it to one stable
 * container per side. No-op when reduce-motion is on or scopes are missing (previews).
 */
@OptIn(ExperimentalSharedTransitionApi::class)
fun Modifier.courseSharedBounds(courseId: Int): Modifier = composed {
    val sharedScope = LocalNavSharedTransitionScope.current
    val visibilityScope = LocalNavAnimatedVisibilityScope.current
    val reduceMotion = LocalReduceMotion.current
    if (sharedScope == null || visibilityScope == null || reduceMotion) {
        this
    } else {
        with(sharedScope) {
            sharedBounds(
                sharedContentState = rememberSharedContentState(key = "course-$courseId"),
                animatedVisibilityScope = visibilityScope,
                enter = fadeIn(WeBeepMotion.enter()),
                exit = fadeOut(WeBeepMotion.exit()),
                boundsTransform = { _, _ -> WeBeepMotion.enter(WeBeepMotion.LongMs) },
                resizeMode = SharedTransitionScope.ResizeMode.RemeasureToBounds,
                clipInOverlayDuringTransition = OverlayClip(CourseContainerShape),
            )
        }
    }
}
