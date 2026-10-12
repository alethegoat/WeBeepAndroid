package it.polimi.webeep.feature.fileviewer.domain

import java.io.File
import kotlinx.coroutines.flow.Flow

/** Source of the bytes behind a [ViewerFile]; the viewer never needs to know where they come from. */
interface FileContentRepository {

    /** Emits download progress, then the classified content or a failure. */
    fun load(file: ViewerFile): Flow<LoadedContent>

    /** Local copy of [file] for sharing or opening elsewhere; downloads it if needed. */
    suspend fun localCopy(file: ViewerFile): File

    /** MIME type to hand to other apps, inferred from the extension when Moodle is vague. */
    fun resolveMimeType(file: ViewerFile): String
}
