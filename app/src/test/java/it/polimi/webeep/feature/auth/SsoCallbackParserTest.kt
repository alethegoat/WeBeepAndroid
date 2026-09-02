package it.polimi.webeep.feature.auth

import it.polimi.webeep.feature.auth.domain.SsoCallbackParser
import org.junit.Assert.assertEquals
import org.junit.Test

class SsoCallbackParserTest {

    @Test
    fun `keeps base64 slashes from moodle token callback`() {
        val payload = "abc/def+ghi=="
        val parsed = SsoCallbackParser.extractToken("webeepandroid://token=$payload")
        assertEquals(payload, parsed)
    }

    @Test
    fun `reads query parameter style token`() {
        val payload = "abc/def+ghi=="
        val parsed = SsoCallbackParser.extractToken("webeepandroid://callback?token=$payload")
        assertEquals(payload, parsed)
    }
}
