package it.polimi.webeep.feature.fileviewer.domain

import java.io.File
import kotlinx.coroutines.flow.Flow

/** Source of the bytes behind a [ViewerFile]; the viewer never needs to know where they come from. */
interface FileContentRepository {

    /**
     * Emits [LoadedContent.Downloading] progress, then exactly one terminal value: the classified content,
     * [LoadedContent.CorruptPdf] or [LoadedContent.Failed]. Errors are values, never thrown. Cancelling the
     * collector aborts the download.
     */
    fun load(file: ViewerFile): Flow<LoadedContent>

    /** Local copy of [file] for sharing or opening elsewhere; downloads it if needed. Throws on failure, unlike [load]. */
    suspend fun localCopy(file: ViewerFile): File

    /** MIME type to hand to other apps, inferred from the extension when Moodle is vague. */
    fun resolveMimeType(file: ViewerFile): String
}
