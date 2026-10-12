package it.polimi.webeep.navigation

import kotlinx.serialization.Serializable

/**
 * Type-safe destinations. Routes carry only what a screen cannot look up itself, and
 * never a token: URLs go through `stripAuthFromUrl` before they are put in a route.
 */
// The SSO callback token reaches the login screen through SsoCallbackRelay, never the route.
@Serializable
data object Login

@Serializable
data object Dashboard

@Serializable
data object Settings

@Serializable
data class CourseDetail(val courseId: Int)

/** [courseId] is null when the file is not opened from a course: no siblings to swipe through. */
@Serializable
data class FileViewer(val url: String, val title: String, val mimeType: String? = null, val courseId: Int? = null)
