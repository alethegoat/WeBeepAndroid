package it.polimi.webeep.feature.dashboard.data

import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.database.entity.CourseEntity
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.di.DefaultDispatcher
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.util.MoodleText
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.model.Course
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Dashboard repository backed by Room and the Moodle REST API.
 */
class DashboardRepositoryImpl @Inject constructor(
    private val apiService: MoodleApiService,
    private val courseDao: CourseDao,
    private val tokenManager: TokenManager,
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
            tokenManager.getToken() ?: throw IllegalStateException("Token non disponibile. Effettua il login.")

            var userId = tokenManager.getUserId()
            if (userId == null) {
                val siteInfo = apiService.getSiteInfo()
                tokenManager.updateUserProfile(siteInfo.userid, siteInfo.fullname)
                userId = siteInfo.userid
            }

            val courseDtos = apiService.getEnrolledCourses(userId = userId)

            val entities = courseDtos.map { dto ->
                val imageUrl = dto.overviewfiles?.firstOrNull()?.fileurl?.let { url ->
                    MoodleFileUrlBuilder.stripAuthFromUrl(url)
                }

                CourseEntity(
                    id = dto.id,
                    shortname = MoodleText.parse(dto.shortname),
                    fullname = MoodleText.parse(dto.fullname),
                    displayname = MoodleText.parse(dto.displayname),
                    summary = MoodleText.parse(dto.summary),
                    startdate = dto.startdate,
                    enddate = dto.enddate,
                    progress = dto.progress,
                    courseImage = imageUrl,
                    category = dto.category?.toString(),
                    isFavourite = dto.isfavourite ?: false,
                    lastSyncTimestamp = System.currentTimeMillis()
                )
            }

            courseDao.replaceAllCourses(entities)

            entities.map { it.toDomain() }
        }
    }

    override suspend fun getUserInfo(): Result<Pair<String, String>> {
        return runCatching {
            tokenManager.getToken() ?: throw IllegalStateException("Token non disponibile")
            val siteInfo = apiService.getSiteInfo()

            tokenManager.updateUserProfile(siteInfo.userid, siteInfo.fullname)

            val avatarUrl = MoodleFileUrlBuilder.stripAuthFromUrl(siteInfo.userpictureurl)
            siteInfo.fullname to avatarUrl
        }
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
