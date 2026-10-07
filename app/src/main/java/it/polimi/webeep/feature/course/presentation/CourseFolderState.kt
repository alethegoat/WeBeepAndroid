package it.polimi.webeep.feature.course.presentation

/** Expansion key of a folder module itself (the root of its tree). */
internal fun folderRootKey(moduleId: Int): String = "$moduleId:"

/** Expansion key of a sub-folder inside a module, e.g. `12:/Esercizi/Soluzioni`. */
internal fun folderKey(moduleId: Int, fullPath: String): String = "$moduleId:$fullPath"

/**
 * Keys of every folder (module root included) on the path to a file whose
 * name contains [query], so search results are visible without extra taps.
 */
internal fun foldersWithMatches(sections: List<SectionUiModel>, query: String): Set<String> {
    val needle = query.trim()
    if (needle.isEmpty()) return emptySet()
    return sections
        .flatMap { it.modules }
        .flatMap { module ->
            module.files
                .filter { it.filename.contains(needle, ignoreCase = true) }
                .flatMap { file -> folderKeysOnPath(module.id, file.filepath) }
        }
        .toSet()
}

/** The module root plus every folder from the root down to [filepath]. */
private fun folderKeysOnPath(moduleId: Int, filepath: String): List<String> {
    val segments = filepath.trim('/').split('/').filter { it.isNotEmpty() }
    val paths = segments.runningReduce { path, segment -> "$path/$segment" }
    return listOf(folderRootKey(moduleId)) + paths.map { folderKey(moduleId, "/$it") }
}
