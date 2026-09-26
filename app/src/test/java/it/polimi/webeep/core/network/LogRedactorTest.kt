package it.polimi.webeep.core.network

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LogRedactorTest {

    @Test
    fun `redacts wstoken in url query`() {
        val line = "--> GET https://webeep.polimi.it/webservice/rest/server.php?wsfunction=f&wstoken=abc123"
        val redacted = LogRedactor.redact(line)
        assertFalse(redacted.contains("abc123"))
        assertEquals(
            "--> GET https://webeep.polimi.it/webservice/rest/server.php?wsfunction=f&wstoken=<REDACTED>",
            redacted
        )
    }

    @Test
    fun `redacts wstoken in form body`() {
        val body = "wsfunction=core_course_get_contents&moodlewsrestformat=json&courseid=5&wstoken=abc123"
        assertEquals(
            "wsfunction=core_course_get_contents&moodlewsrestformat=json&courseid=5&wstoken=<REDACTED>",
            LogRedactor.redact(body)
        )
        assertEquals(
            "wstoken=<REDACTED>&wsfunction=f",
            LogRedactor.redact("wstoken=abc123&wsfunction=f")
        )
    }

    @Test
    fun `redacts pluginfile token and private token`() {
        val url = "https://webeep.polimi.it/webservice/pluginfile.php/1/a.pdf?forcedownload=1&token=abc123"
        assertEquals(
            "https://webeep.polimi.it/webservice/pluginfile.php/1/a.pdf?forcedownload=1&token=<REDACTED>",
            LogRedactor.redact(url)
        )
        assertEquals("token=<REDACTED>", LogRedactor.redact("token=abc123"))
        assertEquals("private_token=<REDACTED>", LogRedactor.redact("private_token=xyz"))
    }

    @Test
    fun `redacts bearer header and leaves unrelated text untouched`() {
        assertEquals("Authorization: Bearer <REDACTED>", LogRedactor.redact("Authorization: Bearer abc.def"))
        assertEquals("wsfunction=f&courseid=5", LogRedactor.redact("wsfunction=f&courseid=5"))
    }
}
