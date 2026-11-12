package it.polimi.webeep

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeBeepAppTest {

    @Test
    fun `a restored logged-in start without a session means the session ended while away`() {
        assertTrue(sessionEndedWhileAway(restoredStartLoggedIn = true, isLoggedIn = false))
    }

    @Test
    fun `a session that is still there is not reported as ended`() {
        assertFalse(sessionEndedWhileAway(restoredStartLoggedIn = true, isLoggedIn = true))
    }

    @Test
    fun `a cold start or a start at login is not reported as ended`() {
        assertFalse(sessionEndedWhileAway(restoredStartLoggedIn = null, isLoggedIn = false))
        assertFalse(sessionEndedWhileAway(restoredStartLoggedIn = false, isLoggedIn = false))
    }
}
