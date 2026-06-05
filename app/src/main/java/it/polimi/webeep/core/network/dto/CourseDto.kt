package it.polimi.webeep.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * dto for a moodle course.
 * mapped from the response of core_enrol_get_users_courses.
 */
@Serializable
data class CourseDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("shortname") val shortname: String = "",
    @SerialName("fullname") val fullname: String = "",
    @SerialName("displayname") val displayname: String? = null,
    @SerialName("summary") val summary: String? = null,
    @SerialName("summaryformat") val summaryformat: Int? = null,
    @SerialName("startdate") val startdate: Long = 0L,
    @SerialName("enddate") val enddate: Long = 0L,
    @SerialName("visible") val visible: Int? = null,
    @SerialName("progress") val progress: Double? = null,
    @SerialName("completed") val completed: Boolean? = null,
    @SerialName("isfavourite") val isfavourite: Boolean? = null,
    @SerialName("hidden") val hidden: Boolean? = null,
    @SerialName("enrolledusercount") val enrolledusercount: Int? = null,
    @SerialName("lastaccess") val lastaccess: Long? = null,
    @SerialName("overviewfiles") val overviewfiles: List<OverviewFileDto>? = null,
    @SerialName("category") val category: Int? = null
)

/**
 * dto for a course overview file.
 */
@Serializable
data class OverviewFileDto(
    @SerialName("filename") val filename: String? = null,
    @SerialName("filepath") val filepath: String? = null,
    @SerialName("filesize") val filesize: Long? = null,
    @SerialName("fileurl") val fileurl: String? = null,
    @SerialName("mimetype") val mimetype: String? = null
)
