package io.github.dexclub.database.classindex

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.dexclub.database.buildDatabase
import io.github.dexclub.database.classindex.dao.ClassesDao
import io.github.dexclub.database.classindex.entities.ClassesEntity

@Database(
    entities = [
        ClassesEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class ClassIndexDatabase : RoomDatabase() {
    lateinit var databaseAbsolutePath: String
        private set

    abstract fun classesDao(): ClassesDao

    companion object {
        private val instances = mutableMapOf<String, ClassIndexDatabase>()
        private val references = mutableMapOf<String, Int>()

        fun open(dir: String): ClassIndexDatabase {
            val databasePath = "$dir/classes.db"
            return synchronized(this) {
                instances[databasePath]?.also {
                    references[databasePath] = (references[databasePath] ?: 0) + 1
                    return it
                }

                buildDatabase<ClassIndexDatabase>(dir, "classes.db")
                    .also {
                        it.databaseAbsolutePath = databasePath
                        instances[databasePath] = it
                        references[databasePath] = 1
                    }
            }
        }

        fun close(dir: String) {
            val databasePath = "$dir/classes.db"
            synchronized(this) {
                val count = references[databasePath] ?: return
                if (count > 1) {
                    references[databasePath] = count - 1
                    return
                }

                instances.remove(databasePath)?.close()
                references.remove(databasePath)
            }
        }

        fun close() {
            synchronized(this) {
                instances.values.forEach { it.close() }
                instances.clear()
                references.clear()
            }
        }
    }
}
