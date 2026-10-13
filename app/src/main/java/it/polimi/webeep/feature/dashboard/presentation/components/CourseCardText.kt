package it.polimi.webeep.feature.dashboard.presentation.components

import androidx.compose.runtime.Immutable
import it.polimi.webeep.core.common.CourseTitleParser
import it.polimi.webeep.feature.dashboard.presentation.CourseUiModel
import java.util.Locale

/** Display-ready pieces of a Moodle course title. */
@Immutable
internal data class CourseCardText(val code: String, val name: String, val subtitle: String, val initials: String)

private val InitialsStopWords = setOf(
    "and", "for", "the", "with", "of", "in", "to",
    "di", "del", "della", "dei", "delle", "degli", "e", "per", "con", "la", "il", "lo", "le", "gli"
)

internal fun CourseUiModel.toCardText(): CourseCardText {
    val parsed = CourseTitleParser.parse(fullname)
    val code = parsed.code.orEmpty().trim()
    val name = parsed.name.trim().ifBlank { fullname.trim() }.toTitleCaseIfShouting()
    val subtitle = listOfNotNull(parsed.teacher, parsed.academicYear)
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .ifEmpty {
            listOfNotNull(category?.trim()?.takeIf { it.isNotEmpty() && !it.all(Char::isDigit) })
        }
        .joinToString(" · ")
    return CourseCardText(code = code, name = name, subtitle = subtitle, initials = initialsOf(name))
}

private fun initialsOf(name: String): String {
    val words = name.split(Regex("[^\\p{L}\\p{N}]+")).filter { it.isNotEmpty() }
    val meaningful = words.filter { it.lowercase(Locale.ROOT) !in InitialsStopWords }.ifEmpty { words }
    return meaningful
        .take(2)
        .joinToString("") { it.first().uppercase(Locale.getDefault()) }
}

/** Moodle titles often arrive in ALL CAPS: soften them to Title Case, leave mixed case alone. */
private fun String.toTitleCaseIfShouting(): String {
    val letters = filter(Char::isLetter)
    if (letters.isEmpty() || letters != letters.uppercase(Locale.ROOT)) return this
    val locale = Locale.getDefault()
    return lowercase(locale)
        .split(" ")
        .joinToString(" ") { word ->
            word.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
        }
}
