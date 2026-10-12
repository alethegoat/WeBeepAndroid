package it.polimi.webeep.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRecordTest {

    @Test
    fun `record survives a map round trip`() {
        val record = AuthRecord(token = "tok", userId = 42, passports = "1|2")

        assertEquals(record, AuthRecord.fromMap(record.toMap()))
    }

    @Test
    fun `null fields are not stored`() {
        val map = AuthRecord(token = "tok").toMap()

        assertEquals(mapOf(AuthRecord.KEY_WS_TOKEN to "tok"), map)
    }

    @Test
    fun `legacy full name is ignored and dropped on the next write`() {
        val legacy = mapOf(AuthRecord.KEY_WS_TOKEN to "tok", AuthRecord.KEY_USER_ID to "7", "full_name" to "Mario Rossi")

        val record = AuthRecord.fromMap(legacy)

        assertEquals(AuthRecord(token = "tok", userId = 7), record)
        assertEquals(mapOf(AuthRecord.KEY_WS_TOKEN to "tok", AuthRecord.KEY_USER_ID to "7"), record.toMap())
    }

    @Test
    fun `invalid or default user id is read as null`() {
        assertNull(AuthRecord.fromMap(mapOf(AuthRecord.KEY_USER_ID to "abc")).userId)
        assertNull(AuthRecord.fromMap(mapOf(AuthRecord.KEY_USER_ID to "-1")).userId)
    }

    @Test
    fun `empty record has no values`() {
        assertTrue(AuthRecord.EMPTY.isEmpty())
        assertTrue(AuthRecord(passports = "").isEmpty())
        assertFalse(AuthRecord(userId = 1).isEmpty())
    }

    @Test
    fun `toString does not expose credentials`() {
        val text = AuthRecord(token = "secret-token", passports = "123|1").toString()

        assertFalse(text.contains("secret-token"))
        assertFalse(text.contains("123"))
    }
}
