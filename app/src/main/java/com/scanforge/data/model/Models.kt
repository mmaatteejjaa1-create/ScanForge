package com.scanforge.data.model

import androidx.room.*

// ─── ScannedModel entity ──────────────────────────────────────────────────────
@Entity(tableName = "scanned_models")
data class ScannedModel(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val filePath: String,
    val mode: String,           // "photogrammetry" | "depth"
    val fileSizeKb: Int,
    val vertexCount: Int = 0,
    val faceCount: Int = 0,
    val hasAr: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val thumbnailPath: String = ""
)

// ─── ModelInfo (for UI display) ───────────────────────────────────────────────
data class ModelInfo(
    val id: Long,
    val name: String,
    val filePath: String,
    val fileSizeKb: Int,
    val vertexCount: Int,
    val faceCount: Int,
    val mode: String
)

// ─── Processing step ──────────────────────────────────────────────────────────
data class ProcessingStepInfo(
    val label: String,
    val isDone: Boolean = false
)

// ─── DAO ──────────────────────────────────────────────────────────────────────
@Dao
interface ScannedModelDao {
    @Query("SELECT * FROM scanned_models ORDER BY createdAt DESC")
    fun getAllModels(): kotlinx.coroutines.flow.Flow<List<ScannedModel>>

    @Query("SELECT * FROM scanned_models ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentModels(limit: Int = 10): kotlinx.coroutines.flow.Flow<List<ScannedModel>>

    @Query("SELECT COUNT(*) FROM scanned_models")
    fun getCount(): kotlinx.coroutines.flow.Flow<Int>

    @Query("SELECT * FROM scanned_models WHERE id = :id")
    suspend fun getById(id: Long): ScannedModel?

    @Query("SELECT * FROM scanned_models WHERE filePath = :path LIMIT 1")
    suspend fun getByPath(path: String): ScannedModel?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(model: ScannedModel): Long

    @Update
    suspend fun update(model: ScannedModel)

    @Delete
    suspend fun delete(model: ScannedModel)

    @Query("DELETE FROM scanned_models WHERE id = :id")
    suspend fun deleteById(id: Long)
}

// ─── Database ─────────────────────────────────────────────────────────────────
@Database(
    entities = [ScannedModel::class],
    version = 1,
    exportSchema = false
)
abstract class ScanForgeDatabase : RoomDatabase() {
    abstract fun modelDao(): ScannedModelDao

    companion object {
        const val DATABASE_NAME = "scanforge.db"
    }
}
