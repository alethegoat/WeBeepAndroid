package it.polimi.webeep.core.common

/** Cleaning of the user-visible strings Moodle returns (multi-language tags, HTML markup and entities). */
object MoodleText {
    private val singleMlangRegex = """\{mlang\s+([a-zA-Z0-9_-]+)\}(.*?)\{mlang\}""".toRegex(RegexOption.DOT_MATCHES_ALL)

    /**
     * Plain-text fields (course, section and module names): resolves the multi-language tags and unescapes the
     * entities Moodle puts in them (`&amp;` for `&`), so the result can be shown as is.
     *
     * Not for HTML fields: unescaping before the markup is stripped would turn an escaped `&lt;` into a tag.
     * Use [resolveLanguage] there and let [cleanHtml] unescape.
     */
    fun parse(text: String?, preferredLang: String = "it"): String = unescapeHtml(resolveLanguage(text, preferredLang))

    /**
     * Resolves Moodle multi-language tags (`{mlang xx}...{mlang}`) to a single language, leaving markup and entities alone.
     * Picks the block matching [preferredLang], or the first available one if there is no match.
     */
    fun resolveLanguage(text: String?, preferredLang: String = "it"): String {
        if (text.isNullOrEmpty()) return ""
        if (!text.contains("{mlang")) return text

        val tags = singleMlangRegex.findAll(text).toList()
        if (tags.isEmpty()) return text

        val hasPreferredLang = tags.any { it.groupValues[1] == preferredLang }
        val firstLang = tags.firstOrNull()?.groupValues?.get(1)
        val langToKeep = if (hasPreferredLang) preferredLang else firstLang

        return text.replace(singleMlangRegex) { matchResult ->
            val lang = matchResult.groupValues[1]
            val content = matchResult.groupValues[2]
            if (lang == langToKeep) content else ""
        }.trim()
    }

    private val HTML_TAG_REGEX = Regex("<[^>]*>")

    /**
     * Strips HTML tags, unescapes common entities and collapses whitespace; blank input gives `""`.
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
