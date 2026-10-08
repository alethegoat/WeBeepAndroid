package it.polimi.webeep.feature.course.presentation

import androidx.compose.runtime.Immutable
import it.polimi.webeep.core.ui.components.GroupPosition
import it.polimi.webeep.core.ui.components.groupPosition

/** Moodle module types that are links rather than files. */
private val LinkModuleTypes = setOf("url", "lti", "webex")

/** How a module row looks and what tapping it does. */
enum class ModuleKind { Folder, Link, SingleFile, MultiFile, Other }

internal fun ModuleUiModel.kind(): ModuleKind = when {
    type == "folder" -> ModuleKind.Folder
    type in LinkModuleTypes -> ModuleKind.Link
    files.size == 1 -> ModuleKind.SingleFile
    files.size > 1 -> ModuleKind.MultiFile
    else -> ModuleKind.Other
}

/**
 * One row of the course content list. Sections, modules, folders and files are
 * flattened so every row is its own lazy item: only visible rows are composed,
 * and expanding a section or folder just inserts rows. [position] places the row
 * inside its section group.
 */
@Immutable
sealed interface CourseListItem {
    val key: String
    val position: GroupPosition
    val contentType: String

    data class SectionHeader(
        override val key: String,
        override val position: GroupPosition,
        val sectionId: Int,
        val name: String,
        val summary: String?,
        val isExpanded: Boolean,
    ) : CourseListItem {
        override val contentType get() = "section_header"
    }

    /** Placeholder row of an expanded section without materials. */
    data class SectionEmpty(
        override val key: String,
        override val position: GroupPosition,
    ) : CourseListItem {
        override val contentType get() = "section_empty"
    }

    data class Module(
        override val key: String,
        override val position: GroupPosition,
        val module: ModuleUiModel,
        val kind: ModuleKind,
        val isFolderExpanded: Boolean,
    ) : CourseListItem {
        override val contentType get() = "module"
    }

    data class Subfolder(
        override val key: String,
        override val position: GroupPosition,
        val name: String,
        val folderKey: String,
        val depth: Int,
        val isExpanded: Boolean,
    ) : CourseListItem {
        override val contentType get() = "subfolder"
    }

    data class File(
        override val key: String,
        override val position: GroupPosition,
        val file: FileUiModel,
        val depth: Int,
    ) : CourseListItem {
        override val contentType get() = "file"
    }
}

/** Rows to show plus whether a search hid every section. */
@Immutable
data class CourseListContent(
    val items: List<CourseListItem>,
    val noMatches: Boolean,
)

/**
 * Flattens [sections] into list rows. While [query] is set only matching
 * sections/modules are kept, and the folders leading to matching files open on
 * their own. Multi-file modules always show their files; folders show theirs
 * when their key is in [expandedFolders].
 */
internal fun buildCourseListItems(
    sections: List<SectionUiModel>,
    expandedFolders: Set<String>,
    query: String,
): CourseListContent {
    val visibleSections = filterSections(sections, query)
    val openFolders = expandedFolders + foldersWithMatches(visibleSections, query)
    val builder = CourseListBuilder(openFolders)
    visibleSections.forEach(builder::addSection)
    return CourseListContent(
        items = builder.items,
        noMatches = visibleSections.isEmpty() && sections.isNotEmpty(),
    )
}

private class CourseListBuilder(private val openFolders: Set<String>) {
    val items = mutableListOf<CourseListItem>()
    private val usedKeys = HashSet<String>()

    /** Rows of the current section; the position is known only once it is complete. */
    private val group = mutableListOf<(GroupPosition) -> CourseListItem>()

    fun addSection(section: SectionUiModel) {
        group.clear()
        val headerKey = uniqueKey("section:${section.id}")
        group += { position ->
            CourseListItem.SectionHeader(
                key = headerKey,
                position = position,
                sectionId = section.id,
                name = section.name,
                summary = section.summary,
                isExpanded = section.isExpanded,
            )
        }
        if (section.isExpanded) {
            if (section.modules.isEmpty()) {
                val emptyKey = uniqueKey("section-empty:${section.id}")
                group += { position -> CourseListItem.SectionEmpty(emptyKey, position) }
            } else {
                section.modules.forEach(::addModule)
            }
        }
        group.forEachIndexed { index, row -> items += row(groupPosition(index, group.size)) }
    }

    private fun addModule(module: ModuleUiModel) {
        val kind = module.kind()
        val isFolderExpanded = kind == ModuleKind.Folder && folderRootKey(module.id) in openFolders
        val key = uniqueKey("module:${module.id}")
        group += { position -> CourseListItem.Module(key, position, module, kind, isFolderExpanded) }
        if (isFolderExpanded || kind == ModuleKind.MultiFile) {
            addTree(buildFolderTree(module.files), module.id, depth = 1)
        }
    }

    private fun addTree(node: FolderNode, moduleId: Int, depth: Int) {
        node.files.forEach { file ->
            val key = uniqueKey("file:$moduleId:${file.filepath}${file.filename}")
            group += { position -> CourseListItem.File(key, position, file, depth) }
        }
        node.subfolders.values.sortedBy { it.name }.forEach { subfolder ->
            val folderKey = folderKey(moduleId, subfolder.fullPath)
            val isExpanded = folderKey in openFolders
            val key = uniqueKey("folder:$folderKey")
            group += { position ->
                CourseListItem.Subfolder(key, position, subfolder.name, folderKey, depth, isExpanded)
            }
            if (isExpanded) addTree(subfolder, moduleId, depth + 1)
        }
    }

    /** Lazy lists crash on duplicate keys: suffix the rare repeat (e.g. same file listed twice). */
    private fun uniqueKey(base: String): String {
        if (usedKeys.add(base)) return base
        var suffix = 1
        while (!usedKeys.add("$base#$suffix")) suffix++
        return "$base#$suffix"
    }
}
