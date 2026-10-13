package it.polimi.webeep.feature.course.presentation

/** User intents the course screen sends to its ViewModel. */
sealed interface CourseDetailAction {
    data object Retry : CourseDetailAction
    data class ToggleSection(val sectionId: Int) : CourseDetailAction
    data class ToggleFolder(val key: String) : CourseDetailAction
    data class SearchQueryChange(val query: String) : CourseDetailAction
    data class PreviewFile(val file: FileUiModel) : CourseDetailAction
    data class DownloadFile(val file: FileUiModel) : CourseDetailAction
    data class OpenFile(val file: FileUiModel) : CourseDetailAction
}
