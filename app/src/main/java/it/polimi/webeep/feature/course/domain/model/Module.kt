package it.polimi.webeep.feature.course.domain.model

data class Module(
    val id: Int,
    val name: String,
    val modName: String, // e.g. "resource", "folder", "url"
    val iconUrl: String?,
    val description: String?,
    val url: String?,
    val files: List<FileResource>
)
