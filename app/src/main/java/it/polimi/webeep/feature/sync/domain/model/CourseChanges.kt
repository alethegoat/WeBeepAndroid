package it.polimi.webeep.feature.sync.domain.model

import kotlinx.serialization.Serializable

/**
 * File changes of one course; [courseName] is stored so a notification can be built without reloading the course.
 *
 * The serialized field names are the on-disk format of existing installs: do not rename them.
 */
@Serializable
data class CourseChanges(val courseId: Int, val courseName: String, val changes: List<FileChange>)
