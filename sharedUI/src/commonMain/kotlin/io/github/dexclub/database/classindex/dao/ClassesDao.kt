package io.github.dexclub.database.classindex.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import io.github.dexclub.database.classindex.entities.ClassesEntity

@Dao
interface ClassesDao {
    @Insert
    suspend fun insert(entity: ClassesEntity): Long

    @Insert
    suspend fun insertAll(entities: List<ClassesEntity>): List<Long>

    @Query("SELECT * FROM classes")
    suspend fun getAll(): List<ClassesEntity>

    @Query("SELECT COUNT(*) FROM classes")
    suspend fun count(): Int

    @Query("DELETE FROM classes")
    suspend fun clear()

    @Query("SELECT * FROM classes WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): ClassesEntity?

    @Query("SELECT * FROM classes WHERE name IN (:names)")
    suspend fun findByNames(names: List<String>): List<ClassesEntity>

    @Query("SELECT * FROM classes WHERE name LIKE '%' || :simpleName")
    suspend fun findBySimpleName(simpleName: String): List<ClassesEntity>
}

