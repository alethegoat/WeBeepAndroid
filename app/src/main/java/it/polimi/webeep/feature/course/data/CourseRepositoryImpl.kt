package it.polimi.webeep.feature.course.data

import it.polimi.webeep.core.database.dao.CourseContentDao
import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.di.DefaultDispatcher
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.model.FileResource
import it.polimi.webeep.feature.course.domain.model.Module
import it.polimi.webeep.feature.course.domain.model.Section
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Course contents repository. Mapping lives in [CourseContentMapper] so the
 * background sync worker cannot drift from the interactive refresh path.
 */
class CourseRepositoryImpl @Inject constructor(
    private val apiService: MoodleApiService,
    private val contentDao: CourseContentDao,
    private val courseDao: CourseDao,
    private val tokenProvider: TokenProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : CourseRepository {

    override suspend fun getCourseContents(courseId: Int): Result<List<Section>> {
        return runCatching {
            tokenProvider.getToken() ?: throw IllegalStateException("token not available. please log in.")

            val sectionDtos = apiService.getCourseContents(courseId = courseId)
            val mapped = withContext(defaultDispatcher) {
                CourseContentMapper.map(courseId = courseId, sectionDtos = sectionDtos)
            }
            contentDao.replaceCourseContent(
                courseId,
                mapped.sectionEntities,
                mapped.moduleEntities,
                mapped.contentEntities
            )
            mapped.sections
        }
    }

    override fun getCachedCourseContents(courseId: Int): Flow<List<Section>> {
        return contentDao.getSectionsByCourse(courseId).map { sectionWithModulesList ->
            sectionWithModulesList.map { swm ->
                val sectionEntity = swm.section
                val modules = swm.modules.map { mwc ->
                    val moduleEntity = mwc.module
                    val files = mwc.contents.map { ce ->
                        FileResource(
                            filename = ce.filename,
                            filepath = ce.filepath ?: "/",
                            fileUrl = MoodleFileUrlBuilder.stripAuthFromUrl(ce.fileUrl),
                            fileSize = ce.fileSize ?: 0L,
                            mimeType = ce.mimeType,
                            timeModified = ce.timeModified
                        )
                    }
                    Module(
                        id = moduleEntity.id,
                        name = moduleEntity.name,
                        modName = moduleEntity.modName,
                        iconUrl = moduleEntity.iconUrl?.let { MoodleFileUrlBuilder.stripAuthFromUrl(it) },
                        description = moduleEntity.description,
                        url = moduleEntity.url?.let { MoodleFileUrlBuilder.stripAuthFromUrl(it) },
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
        }.flowOn(defaultDispatcher)
    }

    override suspend fun getCourseName(courseId: Int): String? {
        return courseDao.getCourseById(courseId)?.fullname
    }
}
