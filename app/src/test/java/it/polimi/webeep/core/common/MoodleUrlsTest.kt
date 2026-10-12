package it.polimi.webeep.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoodleUrlsTest {

    @Test
    fun `stripAuthFromUrl removes token query parameters`() {
        val result = stripAuthFromUrl(
            "https://webeep.polimi.it/webservice/pluginfile.php/1/file.pdf?token=secret&forcedownload=1"
        )

        assertEquals(
            "https://webeep.polimi.it/webservice/pluginfile.php/1/file.pdf?forcedownload=1",
            result
        )
    }

    @Test
    fun `stripAuthFromUrl drops the query entirely when only auth params remain`() {
        assertEquals(
            "https://webeep.polimi.it/webservice/pluginfile.php/1/file.pdf",
            stripAuthFromUrl("https://webeep.polimi.it/pluginfile.php/1/file.pdf?wstoken=secret")
        )
    }

    @Test
    fun `stripAuthFromUrl leaves blank input untouched`() {
        assertEquals("", stripAuthFromUrl(""))
    }
}
