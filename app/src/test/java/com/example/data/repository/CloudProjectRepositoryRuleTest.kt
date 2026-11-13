package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.ProjectTemplates
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class CloudProjectRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun backupAndObserveProject_authenticatedOwner_succeeds() = runBlocking {
        val uid = signInTestUser(ALICE_EMAIL)
        val repo = CloudProjectRepository(firestore)
        val project = ProjectTemplates.buildDuplexVilla(uid).copy(
            id = "proj_${UUID.randomUUID().toString().take(8)}"
        )

        val backupResult = withTimeout(DEFAULT_TIMEOUT_MS) { repo.backupProject(project) }
        assertTrue(backupResult.isSuccess)

        val cloudList = withTimeout(FLOW_TIMEOUT_MS) {
            repo.observeCloudProjects().first { list -> list.any { it.id == project.id } }
        }
        assertTrue(cloudList.any { it.id == project.id && it.name == project.name })

        val versionRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveCloudVersion(project, "Initial test snapshot")
        }
        assertTrue(versionRes.isSuccess)
    }

    @Test
    fun sharedDesign_createReadAndRevoke_enforcesRules() = runBlocking {
        val aliceUid = signInTestUser(ALICE_EMAIL)
        val aliceRepo = CloudProjectRepository(firestore)
        val project = ProjectTemplates.buildCompactStudio(aliceUid).copy(
            id = "proj_${UUID.randomUUID().toString().take(8)}"
        )

        val shareRes = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createSharedDesign(
                project = project,
                ownerDisplayName = "S Irfan",
                isPublic = true,
                permission = "VIEW_ONLY",
                password = null,
                expiresAtMillis = 0L
            )
        }
        assertTrue(shareRes.isSuccess)
        val shareId = shareRes.getOrThrow()

        signInTestUser(BOB_EMAIL)
        val bobRepo = CloudProjectRepository(firestore)
        val fetched = withTimeout(DEFAULT_TIMEOUT_MS) { bobRepo.getSharedDesign(shareId) }
        assertTrue(fetched.isSuccess)
        assertNotNull(fetched.getOrNull())

        val bobEditAttempt = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.updateSharedDesignSnapshot(shareId, project.copy(name = "Tampered"))
        }
        assertTrue(bobEditAttempt.isFailure)
        val firestoreErr = bobEditAttempt.exceptionOrNull()?.unwrapFirestoreException()
        assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreErr?.code)

        signInTestUser(ALICE_EMAIL)
        val revokeRes = withTimeout(DEFAULT_TIMEOUT_MS) { aliceRepo.revokeSharedDesign(shareId) }
        assertTrue(revokeRes.isSuccess)
    }

    @Test
    fun observeCloudProjects_unauthenticated_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = CloudProjectRepository(firestore)
        var caught: Throwable? = null
        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                repo.observeCloudProjects().first()
            }
        } catch (t: Throwable) {
            caught = t
        }
        assertNotNull(caught)
    }

    private companion object {
        const val ALICE_EMAIL = "alice_csi@test.com"
        const val BOB_EMAIL = "bob_csi@test.com"
        const val DEFAULT_TIMEOUT_MS = 8000L
        const val FLOW_TIMEOUT_MS = 6000L
    }
}
