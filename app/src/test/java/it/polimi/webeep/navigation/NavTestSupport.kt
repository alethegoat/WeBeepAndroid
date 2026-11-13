package it.polimi.webeep.navigation

import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.compose.composable
import androidx.navigation.createGraph
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ApplicationProvider

/** Builds a [TestNavHostController] with the app's destinations (empty content) and [start] as start destination. */
internal object NavTestSupport {
    fun <T : Any> newController(start: T): TestNavHostController {
        val controller = TestNavHostController(ApplicationProvider.getApplicationContext())
        controller.navigatorProvider.addNavigator(ComposeNavigator())
        controller.graph = controller.createGraph(startDestination = start) {
            composable<Login> {}
            composable<Dashboard> {}
            composable<Settings> {}
            composable<CourseDetail> {}
            composable<FileViewer> {}
        }
        return controller
    }
}
