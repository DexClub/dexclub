package io.github.dexclub.database

import androidx.room.RoomDatabase
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

expect inline fun <reified T : RoomDatabase> getDatabaseBuilder(path: String, name: String): RoomDatabase.Builder<T>

inline fun <reified T : RoomDatabase> buildDatabase(path: String, name: String): T {
    return getDatabaseBuilder<T>(path, name)
        .setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .fallbackToDestructiveMigration(dropAllTables = true)
        .build()
}
