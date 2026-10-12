package it.polimi.webeep.core.database.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Row of a file inside a module; [fileUrl] is stored without a token (`stripAuthFromUrl`). */
@Entity(
    tableName = "module_contents",
    foreignKeys = [
        ForeignKey(
            entity = CourseModuleEntity::class,
            parentColumns = ["id"],
            childColumns = ["moduleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["moduleId"])]
)
data class ModuleContentEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val moduleId: Int,
    val filename: String,
    val filepath: String?,
    val fileUrl: String,
    val fileSize: Long?,
    val mimeType: String?,
    val timeModified: Long?
)
