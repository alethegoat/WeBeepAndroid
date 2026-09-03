package it.polimi.webeep.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/** Response of core_course_get_enrolled_courses_by_timeline_classification. */
@Serializable
data class TimelineCoursesResponseDto(
    @SerialName("courses") val courses: List<TimelineCourseDto> = emptyList(),
    @SerialName("nextoffset") val nextoffset: Int = 0
)

@Serializable
data class TimelineCourseDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("fullname") val fullname: String = "",
    @SerialName("shortname") val shortname: String = "",
    @SerialName("summary") val summary: String? = null,
    @SerialName("startdate") val startdate: Long = 0L,
    @SerialName("enddate") val enddate: Long = 0L,
    @SerialName("progress") val progress: Double? = null,
    @SerialName("hasprogress") val hasprogress: Boolean? = null,
    @SerialName("isfavourite") val isfavourite: Boolean? = null,
    @SerialName("hidden") val hidden: Boolean? = null,
    @SerialName("courseimage") val courseimage: String? = null,
    @SerialName("coursecategory") val coursecategory: String? = null
)
