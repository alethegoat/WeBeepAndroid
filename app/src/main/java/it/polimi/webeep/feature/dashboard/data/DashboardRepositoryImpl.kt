package it.polimi.webeep.feature.dashboard.data

import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.database.entity.CourseEntity
import it.polimi.webeep.core.datastore.TokenProvider
import it.polimi.webeep.core.di.DefaultDispatcher
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.network.dto.CourseDto
import it.polimi.webeep.core.util.MoodleText
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.model.Course
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Dashboard repository backed by Room and the Moodle REST API.
 */
class DashboardRepositoryImpl @Inject constructor(
    private val apiService: MoodleApiService,
    private val courseDao: CourseDao,
    private val tokenProvider: TokenProvider,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher
) : DashboardRepository {

    override fun getCoursesFromCache(): Flow<List<Course>> {
        return courseDao.getAllCourses().map { entities ->
            entities.map { entity ->
                val courseImage = entity.courseImage?.let { MoodleFileUrlBuilder.stripAuthFromUrl(it) }
                entity.copy(courseImage = courseImage).toDomain()
            }
        }.flowOn(defaultDispatcher)
    }

    override suspend fun syncCourses(): Result<List<Course>> {
        return runCatching {
            tokenProvider.getToken() ?: throw IllegalStateException("Token non disponibile. Effettua il login.")

            var userId = tokenProvider.getUserId()?.takeIf { it > 0 }
            if (userId == null) {
                val siteInfo = apiService.getSiteInfo()
                tokenProvider.updateUserProfile(siteInfo.userid, siteInfo.fullname)
                userId = siteInfo.userid
            }

            val courseDtos = apiService.getEnrolledCourses(userId = userId)

            val syncTimestamp = System.currentTimeMillis()
            val entities = withContext(defaultDispatcher) {
                courseDtos.map { dto -> dto.toEntity(syncTimestamp) }
            }

            courseDao.replaceAllCourses(entities)

            entities.map { it.toDomain() }
        }
    }

    override suspend fun getUserInfo(): Result<Pair<String, String>> {
        return runCatching {
            tokenProvider.getToken() ?: throw IllegalStateException("Token non disponibile")
            val siteInfo = apiService.getSiteInfo()

            tokenProvider.updateUserProfile(siteInfo.userid, siteInfo.fullname)

            val avatarUrl = MoodleFileUrlBuilder.stripAuthFromUrl(siteInfo.userpictureurl)
            siteInfo.fullname to avatarUrl
        }
    }

    private fun CourseDto.toEntity(syncTimestamp: Long): CourseEntity {
        val imageUrl = overviewfiles?.firstOrNull()?.fileurl?.let { url ->
            MoodleFileUrlBuilder.stripAuthFromUrl(url)
        }

        return CourseEntity(
            id = id,
            shortname = MoodleText.parse(shortname),
            fullname = MoodleText.parse(fullname),
            displayname = MoodleText.parse(displayname),
            summary = MoodleText.parse(summary),
            startdate = startdate,
            enddate = enddate,
            progress = progress,
            courseImage = imageUrl,
            category = category?.toString(),
            isFavourite = isfavourite ?: false,
            lastSyncTimestamp = syncTimestamp
        )
    }

    private fun CourseEntity.toDomain() = Course(
        id = id,
        shortname = shortname,
        fullname = fullname,
        summary = summary,
        courseImage = courseImage,
        progress = progress?.toFloat(),
        isFavourite = isFavourite,
        category = category,
        startDate = startdate,
        endDate = enddate
    )
}
