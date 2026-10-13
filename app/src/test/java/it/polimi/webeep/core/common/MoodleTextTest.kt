package it.polimi.webeep.core.common

import org.junit.Assert.assertEquals
import org.junit.Test

class MoodleTextTest {

    @Test
    fun `parses mlang tags selecting preferred language`() {
        val input = "{mlang it}Lezione 1{mlang}{mlang en}Lesson 1{mlang}"
        val parsedIt = MoodleText.parse(input, "it")
        assertEquals("Lezione 1", parsedIt)

        val parsedEn = MoodleText.parse(input, "en")
        assertEquals("Lesson 1", parsedEn)
    }

    @Test
    fun `unescapes common html entities`() {
        val input = "Algebra &amp; Geometria &quot;A.A. 2024&#39;25&quot; &lt;Base&gt;"
        val unescaped = MoodleText.unescapeHtml(input)
        assertEquals("Algebra & Geometria \"A.A. 2024'25\" <Base>", unescaped)
    }

    @Test
    fun `cleanHtml strips tags and unescapes entities`() {
        val html = "<p>Intro to <b>Algorithms</b> &amp; Data Structures.</p><br/><p>Room &nbsp; 3.0.1</p>"
        val cleaned = MoodleText.cleanHtml(html)
        assertEquals("Intro to Algorithms & Data Structures. Room 3.0.1", cleaned)
    }

    @Test
    fun `handles empty and null text gracefully`() {
        assertEquals("", MoodleText.parse(null))
        assertEquals("", MoodleText.parse(""))
        assertEquals("", MoodleText.cleanHtml(null))
        assertEquals("", MoodleText.cleanHtml(""))
        assertEquals("", MoodleText.unescapeHtml(null))
        assertEquals("", MoodleText.unescapeHtml(""))
    }
}
