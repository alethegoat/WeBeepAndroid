package it.polimi.webeep.feature.fileviewer.presentation.components

import androidx.compose.runtime.saveable.SaverScope
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackResumeTest {

    private val scope = SaverScope { true }

    @Test
    fun `the saver round-trips position and pause choice`() {
        val original = PlaybackResume(positionMs = 12_345L, userPaused = true)

        val saved = with(PlaybackResume.Saver) { scope.save(original) }!!

        assertEquals(original, PlaybackResume.Saver.restore(saved))
    }

    @Test
    fun `withPosition keeps the pause choice and takes the player position`() {
        val updated = PlaybackResume(userPaused = true).withPosition(5_000L, ended = false)

        assertEquals(PlaybackResume(positionMs = 5_000L, userPaused = true), updated)
    }

    @Test
    fun `a finished video resumes from the start`() {
        assertEquals(0L, PlaybackResume(positionMs = 9L).withPosition(60_000L, ended = true).positionMs)
    }

    @Test
    fun `an unknown position is clamped to zero`() {
        assertEquals(0L, PlaybackResume().withPosition(-1L, ended = false).positionMs)
    }
}
