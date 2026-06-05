package it.polimi.webeep.core.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * dto for a module inside a course section.
 * represents activities like files, forums, assignments, etc.
 */
@Serializable
data class CourseModuleDto(
    @SerialName("id") val id: Int = 0,
    @SerialName("url") val url: String? = null,
    @SerialName("name") val name: String = "",
    @SerialName("instance") val instance: Int? = null,
    @SerialName("contextid") val contextid: Int? = null,
    @SerialName("visible") val visible: Int? = null,
    @SerialName("uservisible") val uservisible: Boolean? = null,
    @SerialName("modicon") val modicon: String? = null,
    @SerialName("modname") val modname: String = "",
    @SerialName("modplural") val modplural: String? = null,
    @SerialName("indent") val indent: Int? = null,
    @SerialName("description") val description: String? = null,
    @SerialName("contents") val contents: List<ModuleContentDto>? = null
)

/**
 * dto for the content of a module (file, url, etc.).
 */
@Serializable
data class ModuleContentDto(
    @SerialName("type") val type: String = "",
    @SerialName("filename") val filename: String? = null,
    @SerialName("filepath") val filepath: String? = null,
    @SerialName("filesize") val filesize: Long = 0L,
    @SerialName("fileurl") val fileurl: String? = null,
    @SerialName("timecreated") val timecreated: Long? = null,
    @SerialName("timemodified") val timemodified: Long? = null,
    @SerialName("sortorder") val sortorder: Int? = null,
    @SerialName("mimetype") val mimetype: String? = null,
    @SerialName("isexternalfile") val isexternalfile: Boolean? = null,
    @SerialName("author") val author: String? = null,
    @SerialName("license") val license: String? = null
)
