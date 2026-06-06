package it.polimi.webeep.feature.course.data

import it.polimi.webeep.core.database.dao.CourseContentDao
import it.polimi.webeep.core.database.entity.CourseModuleEntity
import it.polimi.webeep.core.database.entity.CourseSectionEntity
import it.polimi.webeep.core.database.entity.ModuleContentEntity
import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import it.polimi.webeep.core.util.MoodleText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * implementation of [CourseRepository] with local serialization support
 * for the modules tree in json format to optimize room storage.
 */
class CourseRepositoryImpl @Inject constructor(
    private val apiService: MoodleApiService,
    private val contentDao: CourseContentDao,
    private val courseDao: CourseDao,
    private val tokenManager: TokenManager
) : CourseRepository {

    private val allowedModules = setOf("resource", "folder", "url", "lti", "webex")

    override suspend fun getCourseContents(courseId: Int): Result<List<Section>> {
        return runCatching {
            val token = tokenManager.getToken() ?: throw IllegalStateException("token not available. please log in.")
            
            // download course contents
            val sectionDtos = apiService.getCourseContents(courseId = courseId)
            
            sectionDtos.forEach { section ->
                section.modules.forEach { mod ->
                    android.util.Log.d("WEBEEP_DEBUG", "Mod: id=${mod.id}, name='${mod.name}', type='${mod.modname}', url='${mod.url}', contents=${mod.contents?.size}")
                    mod.contents?.forEach { content ->
                        android.util.Log.d("WEBEEP_DEBUG", "  Content: filename='${content.filename}', type='${content.type}', fileurl='${content.fileurl}'")
                    }
                }
            }
            
            // filter and map data to domain models
            val sections = sectionDtos.map { sectionDto ->
                val filteredModules = sectionDto.modules
                    .filter { it.modname in allowedModules }
                    .map { moduleDto ->
                        val files = moduleDto.contents?.map { contentDto ->
                            // convert url to webservice/pluginfile.php if it contains /pluginfile.php/
                            val rawUrl = contentDto.fileurl ?: ""
                            val convertedUrl = if (rawUrl.contains("/webservice/pluginfile.php/")) {
                                rawUrl
                            } else if (rawUrl.contains("/pluginfile.php/")) {
                                rawUrl.replace("/pluginfile.php/", "/webservice/pluginfile.php/")
                            } else {
                                rawUrl
                            }

                            FileResource(
                                filename = MoodleText.parse(contentDto.filename ?: "untitled"),
                                filepath = contentDto.filepath ?: "/",
                                fileUrl = MoodleFileUrlBuilder.stripAuthFromUrl(convertedUrl),
                                fileSize = contentDto.filesize,
                                mimeType = contentDto.mimetype,
                                timeModified = contentDto.timemodified
                            )
                        }?.toMutableList() ?: mutableListOf()
                        
                        // if the module is an external link (e.g. webex, lti, url) but has no explicit files,
                        // we transform the module url itself into a clickable file.
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

                        val directUrl = if (moduleDto.modname in setOf("url", "lti", "webex")) {
                            moduleDto.contents?.firstOrNull { it.type == "url" || !it.fileurl.isNullOrBlank() }?.fileurl
                        } else {
                            null
                        }
                        val finalUrl = directUrl ?: moduleDto.url

                        Module(
                            id = moduleDto.id,
                            name = MoodleText.parse(moduleDto.name),
                            modName = moduleDto.modname,
                            iconUrl = moduleDto.modicon,
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

            // delete existing sections and insert the fresh data fetched from the api
            contentDao.deleteSectionsByCourse(courseId)
            contentDao.insertSections(sectionEntities)
            contentDao.insertModules(moduleEntities)
            contentDao.insertContents(contentEntities)

            sections
        }
    }

    override fun getCachedCourseContents(courseId: Int): Flow<List<Section>> {
        return combine(
            contentDao.getSectionsByCourse(courseId),
            tokenManager.wsToken
        ) { sectionWithModulesList, token ->
            sectionWithModulesList.map { swm ->
                val sectionEntity = swm.section
                val modules = swm.modules.map { mwc ->
                    val moduleEntity = mwc.module
                    val files = mwc.contents.map { ce ->
                        // append the ws token if present to authenticate file downloads automatically
                        val fileUrl = if (token != null) {
                            MoodleFileUrlBuilder.buildAuthenticatedUrl(ce.fileUrl, token)
                        } else {
                            ce.fileUrl
                        }
                        FileResource(
                            filename = ce.filename,
                            filepath = ce.filepath ?: "/",
                            fileUrl = fileUrl,
                            fileSize = ce.fileSize ?: 0L,
                            mimeType = ce.mimeType,
                            timeModified = ce.timeModified
                        )
                    }
                    Module(
                        id = moduleEntity.id,
                        name = moduleEntity.name,
                        modName = moduleEntity.modName,
                        iconUrl = moduleEntity.iconUrl,
                        description = moduleEntity.description,
                        url = moduleEntity.url,
                        files = files
                    )
                }

                Section(
                    id = sectionEntity.id,
                    name = sectionEntity.name,
                    summary = sectionEntity.summary,
                    sectionNumber = sectionEntity.section,
                    modules = modules
                )
            }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun getCourseName(courseId: Int): String? {
        return courseDao.getCourseById(courseId)?.fullname
    }
}

