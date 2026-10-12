package it.polimi.webeep.feature.sync.domain.model

import kotlinx.serialization.Serializable

enum class ChangeType { ADDED, UPDATED }

@Serializable
data class FileEntry(val moduleId: Int, val filepath: String, val filename: String, val timeModified: Long, val fileSize: Long)

@Serializable
data class FileChange(val moduleId: Int, val filepath: String, val filename: String, val type: ChangeType)
