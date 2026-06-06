package it.polimi.webeep.feature.dashboard.data

import it.polimi.webeep.core.database.dao.CourseDao
import it.polimi.webeep.core.database.entity.CourseEntity
import it.polimi.webeep.core.datastore.TokenManager
import it.polimi.webeep.core.network.MoodleFileUrlBuilder
import it.polimi.webeep.core.network.MoodleApiService
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.core.util.MoodleText
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject

/**
 * implementation of [DashboardRepository] that integrates room and retrofit.
 */
class DashboardRepositoryImpl @Inject constructor(
    private val apiService: MoodleApiService,
    private val courseDao: CourseDao,
    private val tokenManager: TokenManager
) : DashboardRepository {

    override fun getCoursesFromCache(): Flow<List<Course>> {
        return combine(courseDao.getAllCourses(), tokenManager.wsToken) { entities, token ->
            entities.map { entity ->
                val courseImage = entity.courseImage?.let { url ->
                    if (token != null && url.contains("/pluginfile.php/")) {
                        MoodleFileUrlBuilder.buildAuthenticatedUrl(url, token)
                    } else {
                        url
                    }
                }
                entity.copy(courseImage = courseImage).toDomain()
            }
        }.flowOn(Dispatchers.Default)
    }

    override suspend fun syncCourses(): Result<List<Course>> {
        return runCatching {
            val token = tokenManager.getToken() ?: throw IllegalStateException("Token non disponibile. Effettua il login.")
            
            // retrieves or updates the user id to prepare for the course api call
            var userId = tokenManager.getUserId()
            if (userId == null) {
                val siteInfo = apiService.getSiteInfo(wsToken = token)
                tokenManager.saveAuthData(
                    token = token,
                    privateToken = null, // keeps existing data, but here we set what we have
                    userId = siteInfo.userid,
                    fullName = siteInfo.fullname
                )
                userId = siteInfo.userid
            }

            val courseDtos = apiService.getEnrolledCourses(userId = userId)
            
            val entities = courseDtos.map { dto ->
                val imageUrl = dto.overviewfiles?.firstOrNull()?.fileurl?.let { url ->
                    val authenticated = if (url.contains("/pluginfile.php/")) {
                        MoodleFileUrlBuilder.buildAuthenticatedUrl(url, token)
                    } else {
                        url
                    }
                    MoodleFileUrlBuilder.stripAuthFromUrl(authenticated)
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
            val token = tokenManager.getToken() ?: throw IllegalStateException("Token non disponibile")
            val siteInfo = apiService.getSiteInfo(wsToken = token)
            
            // converts the user image to support the token in case it's a private file
            val avatarUrl = if (siteInfo.userpictureurl.contains("/pluginfile.php/")) {
                MoodleFileUrlBuilder.buildAuthenticatedUrl(siteInfo.userpictureurl, token)
            } else {
                siteInfo.userpictureurl
            }

            tokenManager.saveAuthData(
                token = token,
                privateToken = null,
                userId = siteInfo.userid,
                fullName = siteInfo.fullname
            )

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
