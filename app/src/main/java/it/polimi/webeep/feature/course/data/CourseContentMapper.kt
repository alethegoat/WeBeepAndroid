package it.polimi.webeep.feature.course.data

import it.polimi.webeep.core.common.MoodleText
import it.polimi.webeep.core.database.entity.CourseModuleEntity
import it.polimi.webeep.core.database.entity.CourseSectionEntity
import it.polimi.webeep.core.database.entity.ModuleContentEntity
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.network.dto.CourseModuleDto
import it.polimi.webeep.core.network.dto.CourseSectionDto
import it.polimi.webeep.core.network.dto.ModuleContentDto
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.ModuleType
import it.polimi.webeep.feature.course.domain.model.Section

/**
 * Single mapping from Moodle course-content DTOs to domain models and Room entities.
 * URLs are always persisted without auth query parameters.
 */
object CourseContentMapper {

    data class MappedCourseContent(
        val sections: List<Section>,
        val sectionEntities: List<CourseSectionEntity>,
        val moduleEntities: List<CourseModuleEntity>,
        val contentEntities: List<ModuleContentEntity>
    )

    fun map(courseId: Int, sectionDtos: List<CourseSectionDto>): MappedCourseContent {
        val sections = sectionDtos.map(::mapSection)
        return MappedCourseContent(
            sections = sections,
            sectionEntities = sections.map { toSectionEntity(courseId, it) },
            moduleEntities = sections.flatMap { section -> section.modules.map { toModuleEntity(section.id, it) } },
            contentEntities = sections.flatMap { section ->
                section.modules.flatMap { module -> module.files.map { toContentEntity(module.id, it) } }
            }
        )
    }

    private fun mapSection(sectionDto: CourseSectionDto) = Section(
        id = sectionDto.id,
        name = MoodleText.parse(sectionDto.name),
        summary = MoodleText.parse(sectionDto.summary),
        sectionNumber = sectionDto.section,
        modules = sectionDto.modules
            .filter { it.modname in ModuleType.Supported }
            .map(::mapModule)
    )

    private fun mapModule(moduleDto: CourseModuleDto): Module {
        val finalUrl = (linkTarget(moduleDto) ?: moduleDto.url)?.let { MoodleFileUrlBuilder.stripAuthFromUrl(it) }
        val iconUrl = moduleDto.modicon
            ?.let { MoodleFileUrlBuilder.stripAuthFromUrl(it) }
            ?.takeIf { it.isNotBlank() }

        return Module(
            id = moduleDto.id,
            name = MoodleText.parse(moduleDto.name),
            modName = moduleDto.modname,
            iconUrl = iconUrl,
            description = MoodleText.parse(moduleDto.description),
            url = finalUrl,
            files = mapFiles(moduleDto)
        )
    }

    private fun mapFiles(moduleDto: CourseModuleDto): List<FileResource> {
        val files = if (moduleDto.modname in ModuleType.FileBearing) {
            moduleDto.contents
                ?.filter { content -> content.type != "dir" && !isPhantomIndex(content) }
                ?.map(::toFileResource)
                .orEmpty()
        } else {
            emptyList()
        }
        // A module without files still opens its own page as a pseudo-file.
        return if (files.isEmpty() && !moduleDto.url.isNullOrBlank()) listOf(pageFile(moduleDto, moduleDto.url)) else files
    }

    private fun toFileResource(contentDto: ModuleContentDto) = FileResource(
        filename = MoodleText.parse(contentDto.filename ?: "untitled"),
        filepath = contentDto.filepath ?: "/",
        fileUrl = MoodleFileUrlBuilder.stripAuthFromUrl(contentDto.fileurl ?: ""),
        fileSize = contentDto.filesize,
        mimeType = contentDto.mimetype,
        timeModified = contentDto.timemodified
    )

    private fun pageFile(moduleDto: CourseModuleDto, url: String) = FileResource(
        filename = MoodleText.parse(moduleDto.name),
        filepath = "/",
        fileUrl = MoodleFileUrlBuilder.stripAuthFromUrl(url),
        fileSize = 0L,
        mimeType = "text/html",
        timeModified = 0L
    )

    private fun linkTarget(moduleDto: CourseModuleDto): String? = if (moduleDto.modname in ModuleType.Link) {
        moduleDto.contents
            ?.firstOrNull { it.type == "url" || !it.fileurl.isNullOrBlank() }
            ?.fileurl
    } else {
        null
    }

    private fun toSectionEntity(courseId: Int, section: Section) = CourseSectionEntity(
        id = section.id,
        courseId = courseId,
        name = section.name,
        summary = section.summary,
        section = section.sectionNumber,
        visible = true
    )

    private fun toModuleEntity(sectionId: Int, module: Module) = CourseModuleEntity(
        id = module.id,
        sectionId = sectionId,
        name = module.name,
        modName = module.modName,
        iconUrl = module.iconUrl,
        description = module.description,
        url = module.url
    )

    private fun toContentEntity(moduleId: Int, file: FileResource) = ModuleContentEntity(
        moduleId = moduleId,
        filename = file.filename,
        filepath = file.filepath,
        fileUrl = MoodleFileUrlBuilder.stripAuthFromUrl(file.fileUrl),
        fileSize = file.fileSize,
        mimeType = file.mimeType,
        timeModified = file.timeModified
    )

    private fun isPhantomIndex(content: ModuleContentDto): Boolean = content.filename.equals("index.html", ignoreCase = true) &&
        (content.filesize <= 0L || content.fileurl.isNullOrBlank())
}
