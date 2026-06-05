package it.polimi.webeep.feature.course.domain.model

/**
 * domain model for a module (activity or resource) within a section.
 */
data class Module(
    val id: Int,
    val name: String,
    val modName: String, // e.g. "resource", "folder", "url"
    val iconUrl: String?,
    val description: String?,
    val url: String?,
    val files: List<FileResource>
)
