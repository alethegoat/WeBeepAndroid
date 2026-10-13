package it.polimi.webeep.feature.course.domain.model

/** Moodle `modname` values the app understands, and the groups call sites reason about. */
object ModuleType {
    const val RESOURCE = "resource"
    const val FOLDER = "folder"
    const val URL = "url"
    const val LTI = "lti"
    const val WEBEX = "webex"

    /** Modules that carry downloadable files. */
    val FileBearing = setOf(RESOURCE, FOLDER)

    /** Modules that only point to an external page. */
    val Link = setOf(URL, LTI, WEBEX)

    /** Everything kept when mapping course contents; other module types are dropped. */
    val Supported = FileBearing + Link
}
