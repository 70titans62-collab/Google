package com.example.data.repository

import com.example.data.local.LocalProjectDao
import com.example.data.local.LocalProjectEntity
import com.example.data.local.LocalVersionDao
import com.example.data.local.LocalVersionEntity
import com.example.data.model.ProjectCategory
import com.example.data.model.ProjectDocument
import com.example.data.model.ProjectSerialization
import com.example.data.model.ProjectTemplates
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

class DesignRepository(
    private val projectDao: LocalProjectDao,
    private val versionDao: LocalVersionDao,
    private val cloudRepository: CloudProjectRepository? = null
) {
    fun observeLocalProjects(ownerUid: String): Flow<List<ProjectDocument>> {
        return projectDao.observeAllForUser(ownerUid).map { entities ->
            entities.mapNotNull { entity ->
                ProjectSerialization.fromJson(entity.projectJson)?.copy(
                    id = entity.id,
                    isFavorite = entity.isFavorite,
                    isDeleted = entity.isDeleted,
                    deletedAtMillis = entity.deletedAtMillis,
                    version = entity.version,
                    updatedAtMillis = entity.updatedAtMillis,
                    lastBackupMillis = entity.lastBackupMillis
                )
            }
        }
    }

    fun observeLocalVersions(projectId: String): Flow<List<LocalVersionEntity>> =
        versionDao.observeForProject(projectId)

    suspend fun ensureSeedTemplates(ownerUid: String) {
        if (projectDao.countAll() == 0) {
            val starterVilla = ProjectTemplates.buildDuplexVilla(ownerUid)
            saveProjectLocally(starterVilla, pendingSync = ownerUid != "local_offline", saveVersion = true, versionSummary = "Initial CSI Master Template")
            val starterApt = ProjectTemplates.buildTwoBhkApartment(ownerUid)
            saveProjectLocally(starterApt, pendingSync = ownerUid != "local_offline", saveVersion = true, versionSummary = "Initial 2BHK Template")
        }
    }

    suspend fun saveProjectLocally(
        project: ProjectDocument,
        pendingSync: Boolean = true,
        saveVersion: Boolean = false,
        versionSummary: String = "Auto-saved revision"
    ): ProjectDocument {
        val now = System.currentTimeMillis()
        val updated = project.copy(
            updatedAtMillis = now,
            version = if (saveVersion) project.version + 1 else project.version
        )
        val json = ProjectSerialization.toJson(updated)
        val entity = LocalProjectEntity(
            id = updated.id,
            ownerUid = updated.userId.ifBlank { "local_offline" },
            name = updated.name,
            category = updated.category.name,
            isFavorite = updated.isFavorite,
            isDeleted = updated.isDeleted,
            deletedAtMillis = updated.deletedAtMillis,
            version = updated.version,
            floorCount = updated.floors.size.coerceAtLeast(1),
            projectJson = json,
            updatedAtMillis = now,
            lastBackupMillis = updated.lastBackupMillis,
            pendingCloudSync = pendingSync
        )
        projectDao.upsert(entity)
        if (saveVersion) {
            versionDao.insert(
                LocalVersionEntity(
                    id = "lv_${updated.version}_${UUID.randomUUID().toString().take(6)}",
                    projectId = updated.id,
                    versionNumber = updated.version,
                    summary = versionSummary,
                    snapshotJson = json,
                    savedAtMillis = now
                )
            )
        }
        return updated
    }

    suspend fun getProject(projectId: String): ProjectDocument? {
        val entity = projectDao.getById(projectId) ?: return null
        return ProjectSerialization.fromJson(entity.projectJson)
    }

    suspend fun duplicateProject(project: ProjectDocument, ownerUid: String): ProjectDocument {
        val now = System.currentTimeMillis()
        val copy = project.copy(
            id = "proj_${UUID.randomUUID().toString().take(10)}",
            userId = ownerUid,
            name = "${project.name} (Copy)",
            isDeleted = false,
            deletedAtMillis = 0L,
            version = 1,
            createdAtMillis = now,
            updatedAtMillis = now,
            lastBackupMillis = 0L
        )
        return saveProjectLocally(copy, pendingSync = ownerUid != "local_offline", saveVersion = true, versionSummary = "Duplicated from ${project.name}")
    }

    suspend fun moveToRecycleBin(project: ProjectDocument) {
        val deleted = project.copy(isDeleted = true, deletedAtMillis = System.currentTimeMillis())
        saveProjectLocally(deleted, pendingSync = true)
    }

    suspend fun restoreFromRecycleBin(project: ProjectDocument) {
        val restored = project.copy(isDeleted = false, deletedAtMillis = 0L)
        saveProjectLocally(restored, pendingSync = true)
    }

    suspend fun deletePermanently(projectId: String, isSignedIn: Boolean) {
        projectDao.deletePermanently(projectId)
        versionDao.deleteByProject(projectId)
        if (isSignedIn) {
            cloudRepository?.deleteCloudProject(projectId)
        }
    }

    suspend fun backupProjectToCloud(project: ProjectDocument): Result<ProjectDocument> {
        val repo = cloudRepository ?: return Result.failure(IllegalStateException("Cloud repository unavailable"))
        val res = repo.backupProject(project)
        return if (res.isSuccess) {
            val now = System.currentTimeMillis()
            val backed = project.copy(lastBackupMillis = now)
            saveProjectLocally(backed, pendingSync = false)
            repo.saveCloudVersion(backed, "Cloud Backup v${backed.version}")
            Result.success(backed)
        } else {
            Result.failure(res.exceptionOrNull() ?: Exception("Backup failed"))
        }
    }

    suspend fun restoreSelectedFromCloud(records: List<CloudProjectRecord>, ownerUid: String): Int {
        var count = 0
        for (rec in records) {
            val parsed = ProjectSerialization.fromJson(rec.projectJson) ?: continue
            val restored = parsed.copy(
                id = rec.id.ifBlank { parsed.id },
                userId = ownerUid,
                name = rec.name.ifBlank { parsed.name },
                category = ProjectCategory.entries.find { it.name == rec.category } ?: parsed.category,
                isFavorite = rec.isFavorite,
                isDeleted = rec.isDeleted,
                deletedAtMillis = rec.deletedAtMillis,
                version = rec.version.toInt().coerceAtLeast(1),
                lastBackupMillis = rec.updatedAt?.toDate()?.time ?: System.currentTimeMillis()
            )
            saveProjectLocally(restored, pendingSync = false, saveVersion = true, versionSummary = "Restored from Google Cloud Backup")
            count++
        }
        return count
    }
}
