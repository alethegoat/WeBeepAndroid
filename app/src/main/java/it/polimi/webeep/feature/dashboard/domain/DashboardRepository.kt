package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.feature.dashboard.domain.model.Course
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getCoursesFromCache(): Flow<List<Course>>

    suspend fun syncCourses(): Result<List<Course>>

    suspend fun getUserInfo(): Result<Pair<String, String>>
}
