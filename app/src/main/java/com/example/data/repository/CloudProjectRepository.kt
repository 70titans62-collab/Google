package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.ProjectDocument
import com.example.data.model.ProjectSerialization
import com.example.firebase.OperationType
import com.example.firebase.handleFirestoreError
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.security.MessageDigest
import java.util.UUID

@IgnoreExtraProperties
data class CloudProjectRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val category: String = "",
    val isFavorite: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAtMillis: Long = 0L,
    val version: Long = 1L,
    val floorCount: Long = 1L,
    val folderPath: String = "",
    val projectJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CloudVersionRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val projectId: String = "",
    val versionNumber: Long = 1L,
    val summary: String = "",
    val snapshotJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SharedDesignRecord(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerDisplayName: String = "",
    val projectId: String = "",
    val projectName: String = "",
    val isPublic: Boolean = true,
    val permission: String = "VIEW_ONLY",
    val passwordProtected: Boolean = false,
    val passwordHash: String = "",
    val expiresAtMillis: Long = 0L,
    val revoked: Boolean = false,
    val collaboratorUids: List<String> = emptyList(),
    val snapshotJson: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ShareCommentRecord(
    @DocumentId val id: String = "",
    val shareId: String = "",
    val authorId: String = "",
    val authorName: String = "",
    val text: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

class CloudProjectRepository(private val db: FirebaseFirestore) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String {
        return auth.currentUser?.uid
            ?: throw IllegalStateException("Google Sign-In required before accessing cloud storage.")
    }

    fun observeCloudProjects(): Flow<List<CloudProjectRecord>> = flow {
        val uid = requireUserId()
        val path = "users/$uid/projects"
        emitAll(
            db.collection("users").document(uid).collection("projects")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        CloudProjectRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun backupProject(project: ProjectDocument): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid)
            .collection("projects").document(project.id)
        return try {
            val existing = docRef.get().await()
            val json = ProjectSerialization.toJson(project.copy(userId = uid))
            val folderPath = "CSI-HomeCraft3D/Backups/${project.category.name}"
            if (existing.exists()) {
                val updateMap = mapOf(
                    "name" to project.name.take(120).ifBlank { "Untitled Design" },
                    "category" to project.category.name,
                    "isFavorite" to project.isFavorite,
                    "isDeleted" to project.isDeleted,
                    "deletedAtMillis" to project.deletedAtMillis.coerceAtLeast(0L),
                    "version" to project.version.toLong().coerceAtLeast(1L),
                    "floorCount" to project.floors.size.toLong().coerceIn(1L, 10L),
                    "folderPath" to folderPath,
                    "projectJson" to json,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.update(updateMap).await()
            } else {
                val createMap = mapOf(
                    "userId" to uid,
                    "name" to project.name.take(120).ifBlank { "Untitled Design" },
                    "category" to project.category.name,
                    "isFavorite" to project.isFavorite,
                    "isDeleted" to project.isDeleted,
                    "deletedAtMillis" to project.deletedAtMillis.coerceAtLeast(0L),
                    "version" to project.version.toLong().coerceAtLeast(1L),
                    "floorCount" to project.floors.size.toLong().coerceIn(1L, 10L),
                    "folderPath" to folderPath,
                    "projectJson" to json,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(createMap).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun saveCloudVersion(project: ProjectDocument, summary: String): Result<Unit> {
        val uid = requireUserId()
        val versionId = "v_${project.version}_${UUID.randomUUID().toString().take(6)}"
        val docRef = db.collection("users").document(uid)
            .collection("projects").document(project.id)
            .collection("versions").document(versionId)
        return try {
            val payload = mapOf(
                "userId" to uid,
                "projectId" to project.id,
                "versionNumber" to project.version.toLong().coerceAtLeast(1L),
                "summary" to summary.take(200).ifBlank { "Saved revision" },
                "snapshotJson" to ProjectSerialization.toJson(project.copy(userId = uid)),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchCloudVersions(projectId: String): Result<List<CloudVersionRecord>> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid)
            .collection("projects").document(projectId).collection("versions")
        return try {
            val snap = colRef.get().await()
            val list = snap.toObjects(
                CloudVersionRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            ).sortedByDescending { it.versionNumber }
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun fetchCloudProjectsOnce(): Result<List<CloudProjectRecord>> {
        val uid = requireUserId()
        val colRef = db.collection("users").document(uid).collection("projects")
        return try {
            val snap = colRef.get().await()
            val list = snap.toObjects(
                CloudProjectRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(list)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.LIST, colRef.path)
            Result.failure(e)
        }
    }

    suspend fun deleteCloudProject(projectId: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("users").document(uid).collection("projects").document(projectId)
        return try {
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, docRef.path)
            Result.failure(e)
        }
    }

    // --- Shared Designs & Collaboration ---

    fun observeMySharedDesigns(): Flow<List<SharedDesignRecord>> = flow {
        val uid = requireUserId()
        val path = "shared_designs"
        emitAll(
            db.collection("shared_designs")
                .whereEqualTo("ownerId", uid)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SharedDesignRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observePublicSharedDesigns(): Flow<List<SharedDesignRecord>> = flow {
        val uid = requireUserId()
        val path = "shared_designs"
        emitAll(
            db.collection("shared_designs")
                .whereEqualTo("isPublic", true)
                .whereEqualTo("revoked", false)
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        SharedDesignRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun createSharedDesign(
        project: ProjectDocument,
        ownerDisplayName: String,
        isPublic: Boolean,
        permission: String,
        password: String?,
        expiresAtMillis: Long
    ): Result<String> {
        val uid = requireUserId()
        val shareId = "csi_${UUID.randomUUID().toString().replace("-", "").take(12)}"
        val docRef = db.collection("shared_designs").document(shareId)
        val pwdHash = if (!password.isNullOrBlank()) sha256(password) else ""
        return try {
            val payload = mapOf(
                "ownerId" to uid,
                "ownerDisplayName" to ownerDisplayName.take(80).ifBlank { "S Irfan" },
                "projectId" to project.id,
                "projectName" to project.name.take(120).ifBlank { "Untitled Design" },
                "isPublic" to isPublic,
                "permission" to if (permission == "EDITABLE") "EDITABLE" else "VIEW_ONLY",
                "passwordProtected" to pwdHash.isNotEmpty(),
                "passwordHash" to pwdHash,
                "expiresAtMillis" to expiresAtMillis.coerceAtLeast(0L),
                "revoked" to false,
                "collaboratorUids" to emptyList<String>(),
                "snapshotJson" to ProjectSerialization.toJson(project),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(shareId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun getSharedDesign(shareId: String): Result<SharedDesignRecord?> {
        requireUserId()
        val docRef = db.collection("shared_designs").document(shareId)
        return try {
            val snap = docRef.get().await()
            val record = snap.toObject(
                SharedDesignRecord::class.java,
                DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
            )
            Result.success(record)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun revokeSharedDesign(shareId: String): Result<Unit> {
        requireUserId()
        val docRef = db.collection("shared_designs").document(shareId)
        return try {
            docRef.update(
                mapOf(
                    "revoked" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    suspend fun updateSharedDesignSnapshot(shareId: String, project: ProjectDocument): Result<Unit> {
        requireUserId()
        val docRef = db.collection("shared_designs").document(shareId)
        return try {
            docRef.update(
                mapOf(
                    "snapshotJson" to ProjectSerialization.toJson(project),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            Result.failure(e)
        }
    }

    fun observeShareComments(shareId: String): Flow<List<ShareCommentRecord>> = flow {
        val uid = requireUserId()
        val path = "shared_designs/$shareId/comments"
        emitAll(
            db.collection("shared_designs").document(shareId).collection("comments")
                .snapshots()
                .map { snap ->
                    snap.toObjects(
                        ShareCommentRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val exceptionToLog = firestoreError ?: (error as? Exception)
                    if (exceptionToLog != null && auth.currentUser?.uid == uid) {
                        handleFirestoreError(exceptionToLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addShareComment(shareId: String, authorName: String, text: String): Result<Unit> {
        val uid = requireUserId()
        val docRef = db.collection("shared_designs").document(shareId).collection("comments").document()
        return try {
            val payload = mapOf(
                "shareId" to shareId,
                "authorId" to uid,
                "authorName" to authorName.take(80).ifBlank { "Designer" },
                "text" to text.take(500).trim(),
                "createdAt" to FieldValue.serverTimestamp()
            )
            docRef.set(payload).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            Result.failure(e)
        }
    }

    companion object {
        fun sha256(input: String): String {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
            return bytes.joinToString("") { "%02x".format(it) }
        }
    }
}
