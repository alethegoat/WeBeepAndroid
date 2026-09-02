@file:OptIn(ExperimentalSharedTransitionApi::class)

package it.polimi.webeep.core.ui.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import it.polimi.webeep.core.ui.theme.WeBeepMotion

val LocalNavSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/**
 * Container-transform between dashboard [CourseCard] and course-detail header.
 * No-op when reduce-motion is on or scopes are missing (previews).
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
                boundsTransform = { _, _ -> WeBeepMotion.enter() },
            )
        }
    }
}
