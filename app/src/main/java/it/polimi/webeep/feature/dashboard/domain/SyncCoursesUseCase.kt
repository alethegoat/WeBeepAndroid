package it.polimi.webeep.feature.dashboard.domain

import it.polimi.webeep.feature.dashboard.domain.model.Course
import javax.inject.Inject

class SyncCoursesUseCase @Inject constructor(
    private val repository: DashboardRepository
) {
    suspend operator fun invoke(): Result<List<Course>> {
        return repository.syncCourses()
    }
}
