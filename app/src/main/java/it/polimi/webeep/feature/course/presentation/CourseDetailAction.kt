package it.polimi.webeep.feature.course.presentation

/** User intents the course screen sends to its ViewModel. */
sealed interface CourseDetailAction {
    data object Retry : CourseDetailAction
    data class ToggleSection(val sectionId: Int) : CourseDetailAction
    data class ToggleFolder(val key: String) : CourseDetailAction
    data class SearchQueryChange(val query: String) : CourseDetailAction

    /** Tap on a file: open the viewer or, for web links, a Custom Tab. */
    data class PreviewFile(val file: FileUiModel) : CourseDetailAction

    /** Start or cancel the download of the file. */
    data class DownloadFile(val file: FileUiModel) : CourseDetailAction

    /** Open the downloaded copy in another app. */
    data class OpenFile(val file: FileUiModel) : CourseDetailAction
}
