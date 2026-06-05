package it.polimi.webeep.feature.dashboard.domain.model

/**
 * domain model for a course.
 */
data class Course(
    val id: Int,
    val shortname: String,
    val fullname: String,
    val summary: String?,
    val courseImage: String?,
    val progress: Float?,
    val isFavourite: Boolean,
    val category: String?,
    val startDate: Long,
    val endDate: Long
)
