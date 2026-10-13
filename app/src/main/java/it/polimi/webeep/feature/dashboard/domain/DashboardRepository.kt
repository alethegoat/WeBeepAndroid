package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.feature.dashboard.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface DashboardRepository {
    fun getCoursesFromCache(): Flow<List<Course>>

    suspend fun syncCourses(): Resource<List<Course>>

    suspend fun getUserInfo(): Resource<UserProfile>
}
