package it.polimi.webeep.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe destinations. Routes carry only what a screen cannot look up itself, and
 * never a token: URLs go through `stripAuthFromUrl` before they are put in a route.
 * The SSO callback token reaches the login screen through SsoCallbackRelay, never the route.
 */
@Serializable
data object Login

/** Course list; the start destination when a session exists. */
@Serializable
data object Dashboard

/** Preferences, sync controls and logout. */
@Serializable
data object Settings

/** Sections and files of the course [courseId]. */
@Serializable
data class CourseDetail(val courseId: Int)

/**
 * In-app viewer for one file; [url] must already be token-free.
 * [courseId] is null when the file is not opened from a course: no siblings to swipe through.
 */
@Serializable
data class FileViewer(val url: String, val title: String, val mimeType: String? = null, val courseId: Int? = null)
