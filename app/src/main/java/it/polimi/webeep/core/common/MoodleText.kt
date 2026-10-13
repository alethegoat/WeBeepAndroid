package it.polimi.webeep.core.common

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

        return unescapeHtml(result.trim())
    }

    private val HTML_TAG_REGEX = Regex("<[^>]*>")

    /**
     * Unescapes common HTML entities and strips HTML tags with whitespace normalization.
     */
    fun cleanHtml(text: String?): String {
        if (text.isNullOrBlank()) return ""
        val withoutTags = text.replace(HTML_TAG_REGEX, " ")
        return unescapeHtml(withoutTags).replace(Regex("""\s+"""), " ").trim()
    }

    /**
     * Unescapes common HTML entities present in Moodle titles, summaries, and descriptions.
     */
    fun unescapeHtml(text: String?): String {
        if (text.isNullOrEmpty()) return ""
        return text
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#039;", "'")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&nbsp;", " ")
            .replace("&laquo;", "«")
            .replace("&raquo;", "»")
            .replace("&euro;", "€")
            .replace("&copy;", "©")
            .replace("&reg;", "®")
            .replace("&hellip;", "…")
    }
}
