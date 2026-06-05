package it.polimi.webeep.feature.course.domain.model

/**
 * domain model for a downloadable file in a course module.
 */
data class FileResource(
    val filename: String,
    val filepath: String,
    val fileUrl: String,
    val fileSize: Long,
    val mimeType: String?,
    val timeModified: Long?
)
