package io.github.dexclub.database

import androidx.room.Room
import androidx.room.RoomDatabase
import java.io.File

actual inline fun <reified T : RoomDatabase> getDatabaseBuilder(path: String, name: String): RoomDatabase.Builder<T> {
    val dbFile = File(path, name)
    return Room.databaseBuilder<T>(
        name = dbFile.absolutePath,
    )
}