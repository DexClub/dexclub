package io.github.dexclub.database.classindex.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Serializable
@Entity(tableName = "classes")
data class ClassesEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val signature: String,
    val dexAbsolutePath: String,
    val modifiers: Int = 0,
) {
    val displayName: String
        get() = name.substringAfterLast('.')
}
