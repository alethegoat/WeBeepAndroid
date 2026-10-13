package it.polimi.webeep.feature.dashboard.presentation

import androidx.compose.runtime.Immutable

/** UI model for a course card. [progress] is 0f–1f, null if unavailable. */
@Immutable
data class CourseUiModel(
    val id: Int,
    val shortname: String,
    val fullname: String,
    val courseImage: String? = null,
    val progress: Float? = null,
    val isFavourite: Boolean = false,
    val category: String? = null,
    val endDate: Long = 0L
)
