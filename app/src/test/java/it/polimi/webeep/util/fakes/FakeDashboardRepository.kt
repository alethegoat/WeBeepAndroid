package it.polimi.webeep.util.fakes

import it.polimi.webeep.feature.dashboard.domain.DashboardRepository
import it.polimi.webeep.feature.dashboard.domain.model.Course
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeDashboardRepository : DashboardRepository {
    val cachedCoursesFlow = MutableStateFlow<List<Course>>(emptyList())
    var syncResult: Result<List<Course>> = Result.success(emptyList())
    var userInfoResult: Result<Pair<String, String>> = Result.success("" to "")
    var syncCount = 0

    val syncCalled: Boolean
        get() = syncCount > 0

    override fun getCoursesFromCache(): Flow<List<Course>> = cachedCoursesFlow

    override suspend fun syncCourses(): Result<List<Course>> {
        syncCount++
        return syncResult
    }

    override suspend fun getUserInfo(): Result<Pair<String, String>> = userInfoResult
}
