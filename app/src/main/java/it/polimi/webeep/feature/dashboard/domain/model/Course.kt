package it.polimi.webeep.feature.dashboard.domain.model

/**
 * A course the user is enrolled in.
 *
 * @property fullname Moodle title with multi-language tags resolved; split into parts by `CourseTitleParser` only for display.
 * @property courseImage Token-free overview image URL, `null` when the course has none.
 * @property progress Completion in `0..100`, `null` when Moodle does not track it for the course.
 * @property startDate Course start, as reported by Moodle (0 when unset).
 * @property endDate Course end, as reported by Moodle (0 when unset).
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
