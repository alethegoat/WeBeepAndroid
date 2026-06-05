package it.polimi.webeep.core.util

object MoodleText {
    private val singleMlangRegex = """\{mlang\s+([a-zA-Z0-9_-]+)\}(.*?)\{mlang\}""".toRegex(RegexOption.DOT_MATCHES_ALL)

    /**
     * parse moodle multi-language tags.
     * picks the block matching [preferredlang], or the first available if not found.
     */
    fun parse(text: String?, preferredLang: String = "it"): String {
        if (text.isNullOrEmpty()) return ""
        if (!text.contains("{mlang")) return text

        val tags = singleMlangRegex.findAll(text).toList()
        if (tags.isEmpty()) return text

        val hasPreferredLang = tags.any { it.groupValues[1] == preferredLang }
        val firstLang = tags.firstOrNull()?.groupValues?.get(1)
        val langToKeep = if (hasPreferredLang) preferredLang else firstLang

        var result = text.replace(singleMlangRegex) { matchResult ->
            val lang = matchResult.groupValues[1]
            val content = matchResult.groupValues[2]
            if (lang == langToKeep) content else ""
        }
        
        return result.trim()
    }
}
