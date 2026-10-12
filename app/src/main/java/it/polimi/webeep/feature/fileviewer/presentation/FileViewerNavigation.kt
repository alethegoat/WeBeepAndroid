package it.polimi.webeep.feature.fileviewer.presentation

import androidx.lifecycle.compose.dropUnlessResumed
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import it.polimi.webeep.navigation.FileViewer

/** Registers the viewer destination; back is dropped unless the entry is resumed. */
fun NavGraphBuilder.fileViewerDestination(onBack: () -> Unit) {
    composable<FileViewer> {
        FileViewerRoute(onBackClick = dropUnlessResumed(block = onBack))
    }
}
