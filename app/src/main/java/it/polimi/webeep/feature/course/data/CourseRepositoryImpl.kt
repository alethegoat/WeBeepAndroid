package it.polimi.webeep.feature.course.data

import it.polimi.webeep.core.common.MissingTokenException
import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.core.common.resourceOf
import it.polimi.webeep.core.database.dao.CourseContentDao
import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.di.DefaultDispatcher
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.feature.course.domain.CourseRepository
import it.polimi.webeep.feature.course.domain.model.Section
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

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

    override suspend fun getCourseContents(courseId: Int): Resource<List<Section>> = resourceOf {
        tokenProvider.getToken() ?: throw MissingTokenException()

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

    override fun getCachedCourseContents(courseId: Int): Flow<List<Section>> = contentDao.getSectionsByCourse(courseId)
        .map { sections -> sections.map { it.toDomain() } }
        .flowOn(defaultDispatcher)

    override suspend fun getCourseName(courseId: Int): String? = courseDao.getCourseById(courseId)?.fullname
}
