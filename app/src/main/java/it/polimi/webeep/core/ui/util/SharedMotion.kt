@file:OptIn(ExperimentalSharedTransitionApi::class)

package it.polimi.webeep.core.ui.util

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterExitState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import it.polimi.webeep.core.ui.theme.WeBeepMotion

val LocalNavSharedTransitionScope = staticCompositionLocalOf<SharedTransitionScope?> { null }
val LocalNavAnimatedVisibilityScope = staticCompositionLocalOf<AnimatedVisibilityScope?> { null }

/** Corner radius of [it.polimi.webeep.core.ui.components.CourseCard] (MaterialTheme.shapes.large). */
private val CourseCardCorner = 24.dp

/**
 * Container transform between a dashboard [it.polimi.webeep.core.ui.components.CourseCard]
 * ([isScreen] false) and the whole course-detail screen ([isScreen] true).
 *
 * The screen is measured once at full size and scaled into the animating bounds, so
 * nothing is remeasured per frame. The overlay clip follows the transition, so the
 * card corners morph into the square screen instead of popping at the end. Apply it
 * to one stable container per side. No-op when reduce-motion is on or scopes are
 * missing (previews).
 */
fun Modifier.courseSharedBounds(courseId: Int, isScreen: Boolean): Modifier = composed {
    val sharedScope = LocalNavSharedTransitionScope.current
    val visibilityScope = LocalNavAnimatedVisibilityScope.current
    val reduceMotion = LocalReduceMotion.current
    if (sharedScope == null || visibilityScope == null || reduceMotion) {
        this
    } else {
        val corner = visibilityScope.transition.animateDp(
            transitionSpec = { WeBeepMotion.enter(WeBeepMotion.CONTAINER_TRANSFORM_MS) },
            label = "course_container_corner"
        ) { state ->
            // The card is rounded while it is the one on screen, the screen is square.
            val showingCard = (state == EnterExitState.Visible) != isScreen
            if (showingCard) CourseCardCorner else 0.dp
        }
        val clip = remember(corner) { AnimatedCornerClip(corner) }
        with(sharedScope) {
            sharedBounds(
                sharedContentState = rememberSharedContentState(key = "course-$courseId"),
                animatedVisibilityScope = visibilityScope,
                enter = fadeIn(WeBeepMotion.enter()),
                exit = fadeOut(WeBeepMotion.exit()),
                boundsTransform = { _, _ -> WeBeepMotion.enter(WeBeepMotion.CONTAINER_TRANSFORM_MS) },
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(
                    contentScale = ContentScale.FillWidth,
                    alignment = Alignment.TopCenter
                ),
                clipInOverlayDuringTransition = clip
            )
        }
    }
}

/** Rounded clip whose radius is read at draw time, so animating it never recomposes. */
private class AnimatedCornerClip(private val corner: State<Dp>) : SharedTransitionScope.OverlayClip {
    private val path = Path()

    override fun getClipPath(
        sharedContentState: SharedTransitionScope.SharedContentState,
        bounds: Rect,
        layoutDirection: LayoutDirection,
        density: Density
    ): Path {
        val radius = with(density) { corner.value.toPx() }
        path.reset()
        path.addRoundRect(RoundRect(bounds, CornerRadius(radius)))
        return path
    }
}

/**
 * Returns a check for "the navigation transition of this destination is still
 * running". It is read when called, not during composition, so it never triggers
 * recompositions. Always false outside navigation (previews, tests).
 */
@Composable
fun rememberNavTransitionRunning(): () -> Boolean {
    val visibilityScope = LocalNavAnimatedVisibilityScope.current
    return remember(visibilityScope) { { visibilityScope?.transition?.isRunning == true } }
}
