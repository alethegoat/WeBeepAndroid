package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.feature.dashboard.domain.model.Course
import javax.inject.Inject

/**
 * use case to force network synchronization of courses.
 */
class SyncCoursesUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    suspend operator fun invoke(): Result<List<Course>> {
        // bypass local cache and force a network sync to ensure up-to-date data
        return repository.syncCourses()
    }
}
