package it.polimi.webeep.feature.course.data

import it.polimi.webeep.core.database.entity.CourseModuleEntity
import it.polimi.webeep.core.database.entity.CourseSectionEntity
import it.polimi.webeep.core.database.entity.ModuleContentEntity
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.network.dto.CourseSectionDto
import it.polimi.webeep.core.util.MoodleText
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section

/**
 * Single mapping from Moodle course-content DTOs to domain models and Room entities.
 * URLs are always persisted without auth query parameters.
 */
object CourseContentMapper {

    private val allowedModules = setOf("resource", "folder", "url", "lti", "webex")
    private val fileModules = setOf("resource", "folder")
    private val linkModules = setOf("url", "lti", "webex")

    data class MappedCourseContent(
        val sections: List<Section>,
        val sectionEntities: List<CourseSectionEntity>,
        val moduleEntities: List<CourseModuleEntity>,
        val contentEntities: List<ModuleContentEntity>
    )

    fun map(courseId: Int, sectionDtos: List<CourseSectionDto>): MappedCourseContent {
        val sections = sectionDtos.map { sectionDto ->
            val filteredModules = sectionDto.modules
                .filter { it.modname in allowedModules }
                .map { moduleDto ->
                    val files = if (moduleDto.modname in fileModules) {
                        moduleDto.contents
                            ?.filter { content ->
                                content.type != "dir" && !isPhantomIndex(content)
                            }
                            ?.map { contentDto ->
                                FileResource(
                                    filename = MoodleText.parse(contentDto.filename ?: "untitled"),
                                    filepath = contentDto.filepath ?: "/",
                                    fileUrl = MoodleFileUrlBuilder.stripAuthFromUrl(contentDto.fileurl ?: ""),
                                    fileSize = contentDto.filesize,
                                    mimeType = contentDto.mimetype,
                                    timeModified = contentDto.timemodified
                                )
                            }
                            ?.toMutableList()
                            ?: mutableListOf()
                    } else {
                        mutableListOf()
                    }

                    if (files.isEmpty() && !moduleDto.url.isNullOrBlank()) {
                        files.add(
                            FileResource(
                                filename = MoodleText.parse(moduleDto.name),
                                filepath = "/",
                                fileUrl = MoodleFileUrlBuilder.stripAuthFromUrl(moduleDto.url),
                                fileSize = 0L,
                                mimeType = "text/html",
                                timeModified = 0L
                            )
                        )
                    }

                    val directUrl = if (moduleDto.modname in linkModules) {
                        moduleDto.contents
                            ?.firstOrNull { it.type == "url" || !it.fileurl.isNullOrBlank() }
                            ?.fileurl
                    } else {
                        null
                    }
                    val finalUrl = (directUrl ?: moduleDto.url)?.let { MoodleFileUrlBuilder.stripAuthFromUrl(it) }
                    val iconUrl = moduleDto.modicon
                        ?.let { MoodleFileUrlBuilder.stripAuthFromUrl(it) }
                        ?.takeIf { it.isNotBlank() }

                    Module(
                        id = moduleDto.id,
                        name = MoodleText.parse(moduleDto.name),
                        modName = moduleDto.modname,
                        iconUrl = iconUrl,
                        description = MoodleText.parse(moduleDto.description),
                        url = finalUrl,
                        files = files
                    )
                }

            Section(
                id = sectionDto.id,
                name = MoodleText.parse(sectionDto.name),
                summary = MoodleText.parse(sectionDto.summary),
                sectionNumber = sectionDto.section,
                modules = filteredModules
            )
        }

        val sectionEntities = mutableListOf<CourseSectionEntity>()
        val moduleEntities = mutableListOf<CourseModuleEntity>()
        val contentEntities = mutableListOf<ModuleContentEntity>()

        sections.forEach { section ->
            sectionEntities.add(
                CourseSectionEntity(
                    id = section.id,
                    courseId = courseId,
                    name = section.name,
                    summary = section.summary,
                    section = section.sectionNumber,
                    visible = true
                )
            )

            section.modules.forEach { module ->
                moduleEntities.add(
                    CourseModuleEntity(
                        id = module.id,
                        sectionId = section.id,
                        name = module.name,
                        modName = module.modName,
                        iconUrl = module.iconUrl,
                        description = module.description,
                        url = module.url
                    )
                )

                module.files.forEach { file ->
                    contentEntities.add(
                        ModuleContentEntity(
                            moduleId = module.id,
                            filename = file.filename,
                            filepath = file.filepath,
                            fileUrl = MoodleFileUrlBuilder.stripAuthFromUrl(file.fileUrl),
                            fileSize = file.fileSize,
                            mimeType = file.mimeType,
                            timeModified = file.timeModified
                        )
                    )
                }
            }
        }

        return MappedCourseContent(
            sections = sections,
            sectionEntities = sectionEntities,
            moduleEntities = moduleEntities,
            contentEntities = contentEntities
        )
    }

    private fun isPhantomIndex(content: it.polimi.webeep.core.network.dto.ModuleContentDto): Boolean {
        return content.filename.equals("index.html", ignoreCase = true) &&
            (content.filesize <= 0L || content.fileurl.isNullOrBlank())
    }
}
