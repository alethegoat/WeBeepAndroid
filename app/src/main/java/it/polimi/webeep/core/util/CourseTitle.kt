package it.polimi.webeep.core.util

import java.util.Locale

/**
 * Structured view of a WeBeep/Polimi course full name such as
 * `"052537 - TECHNOLOGIES FOR INFORMATION SYSTEMS (TANCA LETIZIA) [2026-27]"`.
 */
data class CourseTitle(
    val code: String?,
    val name: String,
    val teacher: String?,
    val academicYear: String?
)

/**
 * Splits a raw Moodle course title into code, name, teacher and academic year.
 *
 * Parsing is best effort: every part is optional, and when nothing can be
 * recognised the trimmed raw string is returned as [CourseTitle.name].
 */
object CourseTitleParser {

    private val WHITESPACE = Regex("\\s+")
    private val YEAR_SUFFIX = Regex("^(.*?)\\s*\\[\\s*(\\d{4}\\s*[-/]\\s*\\d{2,4})\\s*]$")
    private val CODE_PREFIX = Regex("^([A-Za-z]{0,4}\\d{3,}[A-Za-z0-9]*)\\s*[-–—]\\s*(.+)$")
    private val TEACHER_SUFFIX = Regex("^(.*\\S)\\s*\\(([^()]*)\\)$")
    private val WORD = Regex("[\\p{L}\\p{N}]+")
    private val HAS_LOWERCASE = Regex("\\p{Ll}")

    private val SMALL_WORDS = setOf(
        // English
        "a", "an", "and", "as", "at", "by", "for", "from", "in", "into", "of", "on", "or",
        "the", "to", "via", "with",
        // Italian
        "e", "ed", "di", "da", "del", "dello", "della", "dei", "degli", "delle", "dell",
        "al", "allo", "alla", "ai", "agli", "alle", "all", "nel", "nello", "nella", "nei",
        "negli", "nelle", "nell", "con", "su", "sul", "sulla", "per", "tra", "fra",
        "il", "lo", "la", "gli", "le", "un", "una", "uno"
    )

    private val UPPERCASE_WORDS = setOf(
        // Roman numerals
        "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII",
        // Common acronyms in course names
        "API", "BIM", "CAD", "CAE", "CAM", "CFD", "CPU", "CRM", "DB", "DBMS", "EMC",
        "ERP", "FEM", "GIS", "GPU", "HCI", "HVAC", "ICT", "IOT", "IT", "LCA", "ML", "MEMS",
        "NLP", "PCB", "PLC", "RF", "SQL", "UI", "UX", "VLSI"
    )

    fun parse(raw: String): CourseTitle {
        val cleaned = raw.trim().replace(WHITESPACE, " ")
        var rest = cleaned

        val yearMatch = YEAR_SUFFIX.matchEntire(rest)
        val academicYear = yearMatch?.let { match ->
            rest = match.groupValues[1]
            match.groupValues[2].replace(WHITESPACE, "")
        }

        val codeMatch = CODE_PREFIX.matchEntire(rest)
        val code = codeMatch?.let { match ->
            rest = match.groupValues[2]
            match.groupValues[1].uppercase(Locale.ROOT)
        }

        val teacherMatch = TEACHER_SUFFIX.matchEntire(rest)
        val teacherRaw = teacherMatch?.let { match ->
            rest = match.groupValues[1]
            match.groupValues[2].trim()
        }

        val matchedSomething = yearMatch != null || codeMatch != null || teacherMatch != null
        val name = rest.trim()
        if (!matchedSomething || name.isEmpty()) {
            return CourseTitle(code = null, name = raw.trim(), teacher = null, academicYear = null)
        }

        return CourseTitle(
            code = code,
            name = toTitleCase(name),
            teacher = teacherRaw?.takeIf { it.isNotEmpty() }?.let(::toNameCase),
            academicYear = academicYear
        )
    }

    /**
     * Title-cases an ALL CAPS course name; mixed-case names are kept verbatim
     * because they were already written with intentional casing.
     */
    private fun toTitleCase(text: String): String {
        if (HAS_LOWERCASE.containsMatchIn(text)) return text
        var isFirst = true
        return WORD.replace(text) { match ->
            val word = match.value
            val lower = word.lowercase(Locale.ROOT)
            val result = when {
                word.any { it.isDigit() } -> word
                word in UPPERCASE_WORDS -> word
                !isFirst && lower in SMALL_WORDS -> lower
                else -> capitalize(lower)
            }
            isFirst = false
            result
        }
    }

    /** Capitalises every word of a person name, e.g. `D'ANGELO MARIO` -> `D'Angelo Mario`. */
    private fun toNameCase(text: String): String {
        if (HAS_LOWERCASE.containsMatchIn(text)) return text
        return WORD.replace(text) { match ->
            val word = match.value
            if (word.any { it.isDigit() }) word else capitalize(word.lowercase(Locale.ROOT))
        }
    }

    private fun capitalize(word: String): String =
        word.replaceFirstChar { it.titlecase(Locale.ROOT) }
}
