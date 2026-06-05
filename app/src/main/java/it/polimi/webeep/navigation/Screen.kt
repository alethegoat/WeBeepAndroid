package it.polimi.webeep.navigation

import android.net.Uri

/**
 * represents the screens and their routes in the navigation graph.
 */
sealed class Screen(val route: String) {
    /**
     * login without sso token (logout, app startup).
     */
    data object Login : Screen("login") {
        const val TOKEN_ARG = "token"
        const val routeWithToken = "login?token={token}"

        fun createRoute(token: String) = "login?token=${Uri.encode(token)}"
    }

    data object Dashboard : Screen("dashboard")

    data object CourseDetail : Screen("course/{courseId}") {
        const val COURSE_ID_ARG = "courseId"
        fun createRoute(courseId: Int) = "course/$courseId"
    }

    data object FileViewer : Screen("fileViewer?url={url}&title={title}&mimeType={mimeType}") {
        const val URL_ARG = "url"
        const val TITLE_ARG = "title"
        const val MIME_TYPE_ARG = "mimeType"

        fun createRoute(url: String, title: String, mimeType: String?) =
            "fileViewer?url=${Uri.encode(url)}&title=${Uri.encode(title)}&mimeType=${Uri.encode(mimeType.orEmpty())}"
    }
}
