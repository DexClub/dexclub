package io.github.dexclub.database.editorsession

import androidx.room.Database
import androidx.room.RoomDatabase
import io.github.dexclub.database.buildDatabase
import io.github.dexclub.database.editorsession.dao.OpenTabDao
import io.github.dexclub.database.editorsession.dao.SidePanelStateDao
import io.github.dexclub.database.editorsession.entities.OpenTabContentEntity
import io.github.dexclub.database.editorsession.entities.OpenTabEntity
import io.github.dexclub.database.editorsession.entities.OpenTabKindPriorityEntity
import io.github.dexclub.database.editorsession.entities.OpenTabPaneEntity
import io.github.dexclub.database.editorsession.entities.SidePanelExpandedPathEntity
import io.github.dexclub.database.editorsession.entities.SidePanelStateEntity

@Database(
    entities = [
        OpenTabEntity::class,
        OpenTabContentEntity::class,
        OpenTabPaneEntity::class,
        OpenTabKindPriorityEntity::class,
        SidePanelStateEntity::class,
        SidePanelExpandedPathEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class EditorSessionDatabase : RoomDatabase() {
    lateinit var databaseAbsolutePath: String
        private set

    abstract fun openTabDao(): OpenTabDao

    abstract fun sidePanelStateDao(): SidePanelStateDao

    companion object {
        private val instances = mutableMapOf<String, EditorSessionDatabase>()
        private val references = mutableMapOf<String, Int>()

        fun open(dir: String): EditorSessionDatabase {
            val databasePath = "$dir/editor_session.db"
            return synchronized(this) {
                instances[databasePath]?.also {
                    references[databasePath] = (references[databasePath] ?: 0) + 1
                    return it
                }

                buildDatabase<EditorSessionDatabase>(dir, "editor_session.db")
                    .also {
                        it.databaseAbsolutePath = databasePath
                        instances[databasePath] = it
                        references[databasePath] = 1
                    }
            }
        }

        fun close(dir: String) {
            val databasePath = "$dir/editor_session.db"
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
