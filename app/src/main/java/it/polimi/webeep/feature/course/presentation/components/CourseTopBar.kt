package it.polimi.webeep.feature.course.presentation.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import it.polimi.webeep.core.ui.components.BackTopBar
import it.polimi.webeep.core.ui.theme.WeBeepMotion
import it.polimi.webeep.core.ui.util.LocalReduceMotion

/**
 * Top bar that is transparent over the header and becomes an opaque
 * surfaceContainer as soon as content scrolls underneath it, so text never
 * shows through. The short course name fades in once the header is gone.
 */
@Composable
internal fun CourseTopBar(title: String, showTitle: Boolean, isScrolled: Boolean, onBackClick: () -> Unit, modifier: Modifier = Modifier) {
    val reduceMotion = LocalReduceMotion.current
    val containerColor by animateColorAsState(
        targetValue = if (isScrolled) MaterialTheme.colorScheme.surfaceContainer else Color.Transparent,
        animationSpec = WeBeepMotion.smooth(reduceMotion = reduceMotion),
        label = "top_bar_container"
    )
    BackTopBar(
        title = {
            AnimatedVisibility(
                visible = showTitle,
                enter = fadeIn(WeBeepMotion.enter(reduceMotion = reduceMotion)),
                exit = fadeOut(WeBeepMotion.exit(reduceMotion = reduceMotion))
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        },
        onBack = onBackClick,
        modifier = modifier,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = containerColor,
            scrolledContainerColor = containerColor,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        )
    )
}
