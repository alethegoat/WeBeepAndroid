package it.polimi.webeep.feature.course.presentation

/**
 * Sections matching [query] by name, summary, module name, description or file name, expanded; a section that
 * matches itself keeps all its modules.
 */
internal fun filterSections(sections: List<SectionUiModel>, query: String): List<SectionUiModel> {
    val needle = query.trim()
    if (needle.isEmpty()) return sections
    return sections.mapNotNull { section ->
        val sectionMatch = section.name.contains(needle, ignoreCase = true) ||
            (section.summary?.contains(needle, ignoreCase = true) == true)
        val filteredModules = section.modules.filter { module ->
            module.name.contains(needle, ignoreCase = true) ||
                (module.description?.contains(needle, ignoreCase = true) == true) ||
                module.files.any { file ->
                    file.filename.contains(needle, ignoreCase = true)
                }
        }
        when {
            sectionMatch -> section.copy(isExpanded = true)
            filteredModules.isNotEmpty() -> section.copy(
                modules = filteredModules,
                isExpanded = true
            )
            else -> null
        }
    }
}
