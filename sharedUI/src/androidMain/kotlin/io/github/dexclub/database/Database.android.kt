package io.github.dexclub.database

import androidx.room.Room
import androidx.room.RoomDatabase
import io.github.dexclub.Env
import java.io.File

actual inline fun <reified T : RoomDatabase> getDatabaseBuilder(path: String, name: String): RoomDatabase.Builder<T> {
    val appContext = Env.application
    val dbFile = File(path, name)
    return Room.databaseBuilder<T>(
        context = appContext,
        name = dbFile.absolutePath,
    )
}