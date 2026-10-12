package it.polimi.webeep.feature.course.domain.model

/**
 * An activity of a course section (file, folder or link).
 *
 * @property modName Moodle module type, one of [ModuleType]'s constants (for example `resource`, `folder`, `url`).
 * @property url Page of the module, or the target for link modules; token-free. `null` when Moodle gave none.
 * @property files Files of the module; a module without files exposes its own page as a single pseudo-file.
 */
data class Module(
    val id: Int,
    val name: String,
    val modName: String,
    val iconUrl: String?,
    val description: String?,
    val url: String?,
    val files: List<FileResource>
)
