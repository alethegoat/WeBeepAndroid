package it.polimi.webeep.feature.course.data

import it.polimi.webeep.core.database.dao.ModuleWithContents
import it.polimi.webeep.core.database.dao.SectionWithModules
import it.polimi.webeep.core.database.entity.ModuleContentEntity
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section

/**
 * Room entity to domain mapping, the counterpart of [CourseContentMapper] (DTO to entity).
 * URLs are stripped again on the way out as a defence in depth against tokens persisted by older versions.
 */
fun SectionWithModules.toDomain(): Section = Section(
    id = section.id,
    name = section.name,
    summary = section.summary,
    sectionNumber = section.section,
    modules = modules.map { it.toDomain() }
)

/** Room module with its files to the domain [Module]; URLs are re-stripped of tokens. */
fun ModuleWithContents.toDomain(): Module = Module(
    id = module.id,
    name = module.name,
    modName = module.modName,
    iconUrl = module.iconUrl?.let { MoodleFileUrlBuilder.stripAuthFromUrl(it) },
    description = module.description,
    url = module.url?.let { MoodleFileUrlBuilder.stripAuthFromUrl(it) },
    files = contents.map { it.toDomain() }
)

/** Room file row to [FileResource]; missing path and size fall back to `/` and `0`. */
fun ModuleContentEntity.toDomain(): FileResource = FileResource(
    filename = filename,
    filepath = filepath ?: "/",
    fileUrl = MoodleFileUrlBuilder.stripAuthFromUrl(fileUrl),
    fileSize = fileSize ?: 0L,
    mimeType = mimeType,
    timeModified = timeModified
)
