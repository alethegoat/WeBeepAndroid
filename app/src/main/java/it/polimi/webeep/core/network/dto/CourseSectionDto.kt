package it.polimi.webeep.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * dto for a moodle course section.
 * mapped from the response of core_course_get_contents.
 */
@Serializable
data class CourseSectionDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("name") val name: String = "",
    @SerialName("visible") val visible: Int? = null,
    @SerialName("summary") val summary: String? = null,
    @SerialName("summaryformat") val summaryformat: Int? = null,
    @SerialName("section") val section: Int = 0,
    @SerialName("hiddenbynumsections") val hiddenbynumsections: Int? = null,
    @SerialName("uservisible") val uservisible: Boolean? = null,
    @SerialName("modules") val modules: List<CourseModuleDto> = emptyList()
)
