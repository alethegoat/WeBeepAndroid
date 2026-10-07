package it.polimi.webeep.navigation

import android.net.Uri

sealed class Screen(val route: String) {
    // The SSO callback token reaches the login screen through SsoCallbackRelay, never the route.
    data object Login : Screen("login")

    data object Dashboard : Screen("dashboard")

    data object Settings : Screen("settings")

    data object CourseDetail : Screen("course/{courseId}") {
        const val COURSE_ID_ARG = "courseId"
        fun createRoute(courseId: Int) = "course/$courseId"
    }

    data object FileViewer : Screen("fileViewer?url={url}&title={title}&mimeType={mimeType}&courseId={courseId}") {
        const val URL_ARG = "url"
        const val TITLE_ARG = "title"
        const val MIME_TYPE_ARG = "mimeType"
        const val COURSE_ID_ARG = "courseId"
        const val NO_COURSE = -1

        fun createRoute(url: String, title: String, mimeType: String?, courseId: Int = NO_COURSE) =
            "fileViewer?url=${Uri.encode(url)}&title=${Uri.encode(title)}" +
                "&mimeType=${Uri.encode(mimeType.orEmpty())}&courseId=$courseId"
    }
}
