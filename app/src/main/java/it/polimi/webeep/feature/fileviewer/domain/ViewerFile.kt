package it.polimi.webeep.feature.fileviewer.domain

/** A file the viewer can show. [url] is always stripped of auth tokens. */
data class ViewerFile(
    val url: String,
    val title: String,
    val mimeType: String,
    /** Size reported by Moodle; 0 when unknown. */
    val size: Long = 0L
)
