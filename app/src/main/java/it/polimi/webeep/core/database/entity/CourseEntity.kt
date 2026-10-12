package it.polimi.webeep.core.database.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Row of an enrolled course.
 *
 * [courseImage] is stored without a token (`stripAuthFromUrl`); tokens never reach Room.
 */
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey val id: Int,
    val shortname: String,
    val fullname: String,
    val displayname: String?,
    val summary: String?,
    val startdate: Long,
    val enddate: Long,
    val progress: Double?,
    val courseImage: String?,
    val category: String?,
    val isFavourite: Boolean,
    val lastSyncTimestamp: Long
)
