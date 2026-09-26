package it.polimi.webeep.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PendingPassportsTest {

    private val now = 1_700_000_000_000L

    @Test
    fun `passport within TTL is kept`() {
        val raw = PendingPassports.serialize(listOf(PendingPassports.Entry("123", now - 60_000L)))

        val entries = PendingPassports.parse(raw, now)

        assertEquals(listOf("123"), entries.map { it.passport })
    }

    @Test
    fun `passport older than TTL is discarded`() {
        val raw = PendingPassports.serialize(
            listOf(
                PendingPassports.Entry("old", now - PendingPassports.TTL_MS),
                PendingPassports.Entry("fresh", now - PendingPassports.TTL_MS + 1)
            )
        )

        val entries = PendingPassports.parse(raw, now)

        assertEquals(listOf("fresh"), entries.map { it.passport })
    }

    @Test
    fun `legacy entries without timestamp and future timestamps are discarded`() {
        val raw = "legacy\nfuture|${now + 60_000L}\nbroken|abc\n"

        assertTrue(PendingPassports.parse(raw, now).isEmpty())
        assertTrue(PendingPassports.parse(null, now).isEmpty())
    }

    @Test
    fun `add moves a repeated passport to the end and caps the list`() {
        var entries = emptyList<PendingPassports.Entry>()
        repeat(PendingPassports.MAX_ENTRIES + 2) { i ->
            entries = PendingPassports.add(entries, "p$i", now + i)
        }
        entries = PendingPassports.add(entries, "p5", now + 100)

        assertEquals(PendingPassports.MAX_ENTRIES, entries.size)
        assertEquals("p5", entries.last().passport)
        assertEquals(1, entries.count { it.passport == "p5" })
        assertEquals(entries, PendingPassports.parse(PendingPassports.serialize(entries), now + 100))
    }
}
