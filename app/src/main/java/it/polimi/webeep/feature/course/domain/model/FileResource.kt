package it.polimi.webeep.feature.course.domain.model

data class FileResource(
    val filename: String,
    val filepath: String,
    val fileUrl: String,
    val fileSize: Long,
    val mimeType: String?,
    val timeModified: Long?
)
