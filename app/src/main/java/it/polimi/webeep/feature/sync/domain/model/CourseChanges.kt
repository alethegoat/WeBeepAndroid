package it.polimi.webeep.feature.sync.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CourseChanges(val courseId: Int, val courseName: String, val changes: List<FileChange>)
