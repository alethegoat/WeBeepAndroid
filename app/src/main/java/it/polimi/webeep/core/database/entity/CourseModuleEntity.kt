package it.polimi.webeep.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Row of a module (activity) inside a section; [url] and [iconUrl] are stored without tokens. */
@Entity(
    tableName = "course_modules",
    foreignKeys = [
        ForeignKey(
            entity = CourseSectionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sectionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sectionId"])]
)
data class CourseModuleEntity(
    @PrimaryKey val id: Int,
    val sectionId: Int,
    val name: String,
    val modName: String,
    val iconUrl: String?,
    val description: String?,
    val url: String? = null
)
