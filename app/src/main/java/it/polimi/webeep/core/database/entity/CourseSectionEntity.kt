package it.polimi.webeep.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "course_sections",
    foreignKeys = [
        ForeignKey(
            entity = CourseEntity::class,
            parentColumns = ["id"],
            childColumns = ["courseId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["courseId"])]
)
data class CourseSectionEntity(
    @PrimaryKey val id: Int,
    val courseId: Int,
    val name: String,
    val summary: String?,
    val section: Int,
    val visible: Boolean
)
