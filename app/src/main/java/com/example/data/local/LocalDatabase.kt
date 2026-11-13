package com.example.data.local

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "local_projects")
data class LocalProjectEntity(
    @PrimaryKey val id: String,
    val ownerUid: String,
    val name: String,
    val category: String,
    val isFavorite: Boolean,
    val isDeleted: Boolean,
    val deletedAtMillis: Long,
    val version: Int,
    val floorCount: Int,
    val projectJson: String,
    val updatedAtMillis: Long,
    val lastBackupMillis: Long,
    val pendingCloudSync: Boolean
)

@Entity(tableName = "local_versions")
data class LocalVersionEntity(
    @PrimaryKey val id: String,
    val projectId: String,
    val versionNumber: Int,
    val summary: String,
    val snapshotJson: String,
    val savedAtMillis: Long
)

@Dao
interface LocalProjectDao {
    @Query("SELECT * FROM local_projects WHERE ownerUid = :ownerUid OR ownerUid = 'local_offline' ORDER BY updatedAtMillis DESC")
    fun observeAllForUser(ownerUid: String): Flow<List<LocalProjectEntity>>

    @Query("SELECT * FROM local_projects WHERE id = :projectId LIMIT 1")
    suspend fun getById(projectId: String): LocalProjectEntity?

    @Query("SELECT * FROM local_projects WHERE pendingCloudSync = 1")
    suspend fun getPendingSync(): List<LocalProjectEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: LocalProjectEntity)

    @Query("DELETE FROM local_projects WHERE id = :projectId")
    suspend fun deletePermanently(projectId: String)

    @Query("SELECT COUNT(*) FROM local_projects")
    suspend fun countAll(): Int
}

@Dao
interface LocalVersionDao {
    @Query("SELECT * FROM local_versions WHERE projectId = :projectId ORDER BY versionNumber DESC")
    fun observeForProject(projectId: String): Flow<List<LocalVersionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: LocalVersionEntity)

    @Query("DELETE FROM local_versions WHERE projectId = :projectId")
    suspend fun deleteByProject(projectId: String)
}

@Database(
    entities = [LocalProjectEntity::class, LocalVersionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class CsiDatabase : RoomDatabase() {
    abstract fun projectDao(): LocalProjectDao
    abstract fun versionDao(): LocalVersionDao

    companion object {
        @Volatile
        private var INSTANCE: CsiDatabase? = null

        fun getInstance(context: Context): CsiDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    CsiDatabase::class.java,
                    "csi_homecraft_db"
                ).build().also { INSTANCE = it }
            }
        }
    }
}
