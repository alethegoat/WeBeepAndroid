package it.polimi.webeep.util.fakes

import it.polimi.webeep.core.common.Resource
import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.model.Course
import it.polimi.webeep.feature.dashboard.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDashboardRepository : DashboardRepository {
    val cachedCoursesFlow = MutableStateFlow<List<Course>>(emptyList())
    var syncResult: Resource<List<Course>> = Resource.Success(emptyList())
    var userInfoResult: Resource<UserProfile> = Resource.Success(UserProfile("", ""))
    var syncCount = 0

    val syncCalled: Boolean
        get() = syncCount > 0

    override fun getCoursesFromCache(): Flow<List<Course>> = cachedCoursesFlow

    override suspend fun syncCourses(): Resource<List<Course>> {
        syncCount++
        return syncResult
    }

    override suspend fun getUserInfo(): Resource<UserProfile> = userInfoResult
}
