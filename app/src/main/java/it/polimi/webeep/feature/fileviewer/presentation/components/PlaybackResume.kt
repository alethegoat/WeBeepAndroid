package it.polimi.webeep.feature.fileviewer.presentation.components

import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

/**
 * Where a video should continue after the player is rebuilt (rotation, process death).
 * The player lives in the composition and is recreated on a configuration change, so this is what survives.
 */
internal data class PlaybackResume(val positionMs: Long = 0L, val userPaused: Boolean = false) {

    /** Same pause choice with the position read from the player; a finished video restarts instead of resuming at its end. */
    fun withPosition(currentPositionMs: Long, ended: Boolean): PlaybackResume =
        copy(positionMs = if (ended) 0L else currentPositionMs.coerceAtLeast(0L))

    companion object {
        val Saver: Saver<PlaybackResume, Any> = listSaver(
            save = { listOf(it.positionMs, it.userPaused) },
            restore = { PlaybackResume(positionMs = it[0] as Long, userPaused = it[1] as Boolean) }
        )
    }
}
