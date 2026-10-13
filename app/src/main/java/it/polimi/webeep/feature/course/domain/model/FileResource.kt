package it.polimi.webeep.feature.course.domain.model

/**
 * A downloadable file or, for modules without files, the module page itself.
 *
 * @property fileUrl Never contains a token (it went through `stripAuthFromUrl`): the token is added only when the
 * request is made.
 * @property timeModified Last modification time as reported by Moodle, `null` when unknown; used to detect updated files.
 */
data class FileResource(
    val filename: String,
    val filepath: String,
    val fileUrl: String,
    val fileSize: Long,
    val mimeType: String?,
    val timeModified: Long?
)
