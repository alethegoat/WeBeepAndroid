package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.feature.dashboard.domain.model.Course
import kotlinx.coroutines.flow.Flow

/**
 * repository for dashboard management.
 */
interface DashboardRepository {
    /**
     * retrieves courses saved in the local cache.
     */
    fun getCoursesFromCache(): Flow<List<Course>>

    /**
     * synchronizes courses with the moodle server and updates the local cache.
     */
    suspend fun syncCourses(): Result<List<Course>>

    /**
     * retrieves authenticated user information (full name, avatar url).
     */
    suspend fun getUserInfo(): Result<Pair<String, String>>
}
