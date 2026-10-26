package com.example.jesuisfather.core.admin

import android.app.Activity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class LockTaskControllerTest {

    private lateinit var fakeKiosk: FakeKioskPolicyManager

    @Before
    fun setup() {
        fakeKiosk = FakeKioskPolicyManager()
    }

    @Test
    fun isDeviceOwner_reflectsState() {
        assertFalse(fakeKiosk.isDeviceOwner)
        fakeKiosk.isDeviceOwner = true
        assertTrue(fakeKiosk.isDeviceOwner)
    }

    @Test
    fun setupKioskPolicies_whitelistsPackagesOnlyWhenDeviceOwner() {
        fakeKiosk.isDeviceOwner = false
        val failed = fakeKiosk.setupKioskPolicies(listOf("com.example.game"))
        assertFalse(failed)
        assertTrue(fakeKiosk.whitelistedPackages.isEmpty())

        fakeKiosk.isDeviceOwner = true
        val success = fakeKiosk.setupKioskPolicies(listOf("com.example.game", "com.example.learn"))
        assertTrue(success)
        assertEquals(2, fakeKiosk.whitelistedPackages.size)
        assertTrue(fakeKiosk.whitelistedPackages.contains("com.example.game"))
        assertTrue(fakeKiosk.isLockTaskPermitted("com.example.game"))
        assertFalse(fakeKiosk.isLockTaskPermitted("com.example.unallowed"))
    }

    @Test
    fun clearDeviceOwner_resetsOwnershipAndWhitelist() {
        fakeKiosk.isDeviceOwner = true
        fakeKiosk.setupKioskPolicies(listOf("com.example.game"))

        assertTrue(fakeKiosk.clearDeviceOwner())
        assertFalse(fakeKiosk.isDeviceOwner)
        assertTrue(fakeKiosk.whitelistedPackages.isEmpty())
    }
}

/**
 * Deterministic test double for KioskPolicyManager.
 */
class FakeKioskPolicyManager : KioskPolicyManager {
    override var isDeviceOwner: Boolean = false
    val whitelistedPackages = mutableSetOf<String>()
    var isKioskActive: Boolean = false

    override fun isLockTaskPermitted(packageName: String): Boolean {
        return whitelistedPackages.contains(packageName)
    }

    override fun setupKioskPolicies(whitelistedPackages: List<String>): Boolean {
        if (!isDeviceOwner) return false
        this.whitelistedPackages.clear()
        this.whitelistedPackages.addAll(whitelistedPackages)
        return true
    }

    override fun startKiosk(activity: Activity): Boolean {
        isKioskActive = true
        return true
    }

    override fun stopKiosk(activity: Activity): Boolean {
        isKioskActive = false
        return true
    }

    override fun clearDeviceOwner(): Boolean {
        if (!isDeviceOwner) return false
        isDeviceOwner = false
        whitelistedPackages.clear()
        return true
    }
}
