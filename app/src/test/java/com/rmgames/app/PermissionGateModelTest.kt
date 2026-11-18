package com.rmgames.app

import androidx.compose.runtime.snapshots.Snapshot
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Decision state of the permission gate: *what to show* while the Android glue
 * (checking/requesting) lives in the composable. See docs/08-implementation-plan.md.
 */
class PermissionGateModelTest {

    private val camera = "android.permission.CAMERA"

    @Test
    fun `no permissions required means content even when nothing is granted`() {
        val model = PermissionGateModel(requiredPermissions = emptyList(), initiallyGranted = false)

        assertTrue(model.isContentVisible)
        assertFalse(model.wasDenied)
    }

    @Test
    fun `all permissions granted initially means content`() {
        val model = PermissionGateModel(requiredPermissions = listOf(camera), initiallyGranted = true)

        assertTrue(model.isContentVisible)
        assertFalse(model.wasDenied)
    }

    @Test
    fun `denied initially means fallback and no denial recorded yet`() {
        val model = PermissionGateModel(requiredPermissions = listOf(camera), initiallyGranted = false)

        assertFalse(model.isContentVisible)
        assertFalse(model.wasDenied)
    }

    @Test
    fun `granting through the system dialog reveals content`() {
        val model = PermissionGateModel(requiredPermissions = listOf(camera), initiallyGranted = false)

        model.onPermissionsResult(mapOf(camera to true))

        assertTrue(model.isContentVisible)
        assertFalse(model.wasDenied)
    }

    @Test
    fun `denial through the system dialog keeps fallback and marks denial`() {
        val model = PermissionGateModel(requiredPermissions = listOf(camera), initiallyGranted = false)

        model.onPermissionsResult(mapOf(camera to false))

        assertFalse(model.isContentVisible)
        assertTrue(model.wasDenied)
    }

    @Test
    fun `partial grant of several permissions keeps fallback`() {
        val model = PermissionGateModel(
            requiredPermissions = listOf("perm.a", "perm.b"),
            initiallyGranted = false,
        )

        model.onPermissionsResult(mapOf("perm.a" to true, "perm.b" to false))

        assertFalse(model.isContentVisible)
        assertTrue(model.wasDenied)
    }

    @Test
    fun `resume after granting in system settings reveals content`() {
        val model = PermissionGateModel(requiredPermissions = listOf(camera), initiallyGranted = false)
        model.onPermissionsResult(mapOf(camera to false))

        model.onHostResumed(granted = true)

        assertTrue(model.isContentVisible)
        assertTrue(model.wasDenied)
    }

    @Test
    fun `resume while still denied keeps fallback with the settings hint`() {
        val model = PermissionGateModel(requiredPermissions = listOf(camera), initiallyGranted = false)
        model.onPermissionsResult(mapOf(camera to false))

        model.onHostResumed(granted = false)

        assertFalse(model.isContentVisible)
        assertTrue(model.wasDenied)
    }

    @Test
    fun `a grant is published to the snapshot system so the gate recomposes`() {
        val model = PermissionGateModel(requiredPermissions = listOf(camera), initiallyGranted = false)

        val applied = snapshotChangesOf {
            model.onPermissionsResult(mapOf(camera to true))
        }

        assertTrue(
            "isContentVisible must be snapshot state: with a plain var PermissionGate " +
                "never recomposes after the user grants the permission",
            applied,
        )
    }

    @Test
    fun `a denial is published to the snapshot system so the hint appears`() {
        val model = PermissionGateModel(requiredPermissions = listOf(camera), initiallyGranted = false)

        val applied = snapshotChangesOf {
            model.onPermissionsResult(mapOf(camera to false))
        }

        assertTrue(
            "wasDenied must be snapshot state or the settings hint never appears",
            applied,
        )
    }

    /** True when [block] wrote at least one Compose snapshot state object. */
    private fun snapshotChangesOf(block: () -> Unit): Boolean {
        val applied = mutableListOf<Any>()
        val observer = Snapshot.registerApplyObserver { state, _ -> applied.add(state) }
        try {
            block()
            Snapshot.sendApplyNotifications()
        } finally {
            observer.dispose()
        }
        return applied.isNotEmpty()
    }
}
