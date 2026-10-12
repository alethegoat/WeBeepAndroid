package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.feature.dashboard.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

/** The user's enrolled courses and profile, from the local cache and from Moodle. */
interface DashboardRepository {
    /** Cached courses, re-emitted when the cache changes; empty before the first sync. */
    fun getCoursesFromCache(): Flow<List<Course>>

    /** Fetches the enrolled courses and replaces the cached list. Looks up and stores the user id when it is not known yet. */
    suspend fun syncCourses(): Resource<List<Course>>

    /** Fetches the signed-in user's name and avatar (token-free URL) from Moodle. */
    suspend fun getUserInfo(): Resource<UserProfile>
}
