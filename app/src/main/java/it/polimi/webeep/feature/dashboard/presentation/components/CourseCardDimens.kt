package it.polimi.webeep.feature.dashboard.presentation.components

import androidx.compose.ui.unit.dp

/**
 * Sizes of the course card that the loading skeleton (CourseCardSkeleton) mirrors, so the
 * placeholder cannot drift from the real card.
 */
internal object CourseCardDimens {
    /** Height of the cover image when a course has one; kept small so text stays the focus. */
    val CoverHeight = 112.dp

    /** Size of the monogram tile shown when a course has no image. */
    val MonogramSize = 56.dp
}
