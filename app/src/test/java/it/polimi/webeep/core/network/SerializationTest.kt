package it.polimi.webeep.core.network

import it.polimi.webeep.core.network.dto.CourseDto
import it.polimi.webeep.core.network.dto.SiteInfoDto
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SerializationTest {

    private val jsonParser = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    @Test
    fun `CourseDto deserializes correctly with missing optional fields`() {
        val payload = """
            {
                "id": 105,
                "shortname": "ALGO-2026",
                "fullname": "Algoritmi e Principi dell'Informatica"
            }
        """.trimIndent()

        val course = jsonParser.decodeFromString<CourseDto>(payload)

        assertEquals(105, course.id)
        assertEquals("ALGO-2026", course.shortname)
        assertEquals("Algoritmi e Principi dell'Informatica", course.fullname)
        
        // Verifica che i valori di default per le chiavi assenti vengano applicati
        assertNull("Summary should be null when missing", course.summary)
        assertEquals(0L, course.startdate)
        assertEquals(0L, course.enddate)
        assertNull("Progress should be null when missing", course.progress)
    }

    @Test
    fun `CourseDto deserializes correctly with extra unknown fields`() {
        val payload = """
            {
                "id": 105,
                "shortname": "ALGO-2026",
                "fullname": "Algoritmi",
                "new_unsupported_field_from_future_moodle_version": "should be ignored"
            }
        """.trimIndent()

        val course = jsonParser.decodeFromString<CourseDto>(payload)

        assertEquals(105, course.id)
        assertEquals("ALGO-2026", course.shortname)
        // Ignora coerentemente la chiave sconosciuta grazie a ignoreUnknownKeys = true
    }

    @Test
    fun `SiteInfoDto parses complex JSON with nested functions gracefully`() {
        val payload = """
            {
                "sitename": "WeBeep Polimi",
                "username": "alessio.studente",
                "firstname": "Alessio",
                "lastname": "Rossi",
                "fullname": "Alessio Rossi",
                "lang": "it",
                "userid": 12345,
                "siteurl": "https://webeep.polimi.it",
                "userpictureurl": "https://webeep.polimi.it/avatar.png",
                "downloadfiles": 1,
                "functions": [
                    {
                        "name": "core_course_get_contents",
                        "version": "1.0"
                    }
                ]
            }
        """.trimIndent()

        val siteInfo = jsonParser.decodeFromString<SiteInfoDto>(payload)

        assertEquals("WeBeep Polimi", siteInfo.sitename)
        assertEquals(12345, siteInfo.userid)
        assertTrue(siteInfo.functions?.isNotEmpty() == true)
        assertEquals("core_course_get_contents", siteInfo.functions?.get(0)?.name)
    }
}
