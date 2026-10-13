package it.polimi.webeep.core.common

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CourseTitleParserTest {

    @Test
    fun `parses full Polimi title`() {
        val title = CourseTitleParser.parse(
            "052537 - TECHNOLOGIES FOR INFORMATION SYSTEMS (TANCA LETIZIA) [2026-27]"
        )

        assertEquals(
            CourseTitle(
                code = "052537",
                name = "Technologies for Information Systems",
                teacher = "Tanca Letizia",
                academicYear = "2026-27"
            ),
            title
        )
    }

    @Test
    fun `keeps trailing number of course name`() {
        val title = CourseTitleParser.parse("089183 - DATA BASES 2 (COMAI SARA) [2025-26]")

        assertEquals("089183", title.code)
        assertEquals("Data Bases 2", title.name)
        assertEquals("Comai Sara", title.teacher)
        assertEquals("2025-26", title.academicYear)
    }

    @Test
    fun `keeps roman numerals uppercase`() {
        val title = CourseTitleParser.parse("051234 - ANALISI MATEMATICA II (ROSSI MARIO) [2026-27]")

        assertEquals("Analisi Matematica II", title.name)
    }

    @Test
    fun `lowercases short Italian and English words except the first`() {
        assertEquals(
            "Fondamenti di Automatica e Controllo",
            CourseTitleParser.parse("012345 - FONDAMENTI DI AUTOMATICA E CONTROLLO").name
        )
        assertEquals(
            "The Art of Computer Programming",
            CourseTitleParser.parse("012345 - THE ART OF COMPUTER PROGRAMMING").name
        )
        assertEquals(
            "Storia dell'Architettura",
            CourseTitleParser.parse("012345 - STORIA DELL'ARCHITETTURA").name
        )
    }

    @Test
    fun `keeps words containing digits and known acronyms uppercase`() {
        assertEquals(
            "Principles of DB2 and SQL",
            CourseTitleParser.parse("012345 - PRINCIPLES OF DB2 AND SQL").name
        )
    }

    @Test
    fun `title-cases compound teacher names`() {
        val title = CourseTitleParser.parse("012345 - FISICA (D'ANGELO MARIA-LUISA) [2026-27]")

        assertEquals("D'Angelo Maria-Luisa", title.teacher)
    }

    @Test
    fun `handles missing code`() {
        val title = CourseTitleParser.parse("DATA BASES 2 (COMAI SARA) [2025-26]")

        assertNull(title.code)
        assertEquals("Data Bases 2", title.name)
        assertEquals("Comai Sara", title.teacher)
        assertEquals("2025-26", title.academicYear)
    }

    @Test
    fun `handles missing teacher`() {
        val title = CourseTitleParser.parse("052537 - TECHNOLOGIES FOR INFORMATION SYSTEMS [2026-27]")

        assertEquals("052537", title.code)
        assertEquals("Technologies for Information Systems", title.name)
        assertNull(title.teacher)
        assertEquals("2026-27", title.academicYear)
    }

    @Test
    fun `handles missing academic year`() {
        val title = CourseTitleParser.parse("052537 - TECHNOLOGIES FOR INFORMATION SYSTEMS (TANCA LETIZIA)")

        assertEquals("052537", title.code)
        assertEquals("Technologies for Information Systems", title.name)
        assertEquals("Tanca Letizia", title.teacher)
        assertNull(title.academicYear)
    }

    @Test
    fun `uses last parenthesis pair before the year as teacher`() {
        val title = CourseTitleParser.parse("054321 - ARCHITETTURA (LABORATORIO) (BIANCHI LUCA) [2026-27]")

        assertEquals("Architettura (Laboratorio)", title.name)
        assertEquals("Bianchi Luca", title.teacher)
        assertEquals("2026-27", title.academicYear)
    }

    @Test
    fun `collapses extra whitespace`() {
        val title = CourseTitleParser.parse(
            "   052537   -   TECHNOLOGIES   FOR INFORMATION  SYSTEMS  ( TANCA  LETIZIA )  [ 2026 - 27 ]  "
        )

        assertEquals("052537", title.code)
        assertEquals("Technologies for Information Systems", title.name)
        assertEquals("Tanca Letizia", title.teacher)
        assertEquals("2026-27", title.academicYear)
    }

    @Test
    fun `accepts full four digit academic year`() {
        assertEquals("2025/2026", CourseTitleParser.parse("012345 - FISICA [2025/2026]").academicYear)
    }

    @Test
    fun `keeps mixed case names verbatim`() {
        val title = CourseTitleParser.parse("052537 - Machine Learning for NLP (Rossi Mario) [2026-27]")

        assertEquals("Machine Learning for NLP", title.name)
        assertEquals("Rossi Mario", title.teacher)
    }

    @Test
    fun `falls back to trimmed raw text when format does not match`() {
        assertEquals(
            CourseTitle(code = null, name = "Corso di prova", teacher = null, academicYear = null),
            CourseTitleParser.parse("  Corso di prova  ")
        )
        assertEquals(
            CourseTitle(code = null, name = "DATA BASES 2", teacher = null, academicYear = null),
            CourseTitleParser.parse("DATA BASES 2")
        )
    }

    @Test
    fun `falls back when only metadata is present`() {
        val title = CourseTitleParser.parse("(TANCA LETIZIA)")

        assertEquals("(TANCA LETIZIA)", title.name)
        assertNull(title.code)
        assertNull(title.teacher)
        assertNull(title.academicYear)
    }

    @Test
    fun `ignores empty teacher parentheses`() {
        val title = CourseTitleParser.parse("012345 - FISICA () [2026-27]")

        assertEquals("Fisica", title.name)
        assertNull(title.teacher)
    }

    @Test
    fun `handles blank input`() {
        assertEquals(CourseTitle(null, "", null, null), CourseTitleParser.parse("   "))
    }

    @Test
    fun `does not treat a plain dash as code separator`() {
        val title = CourseTitleParser.parse("DESIGN - STUDIO [2026-27]")

        assertNull(title.code)
        assertEquals("Design - Studio", title.name)
    }
}
