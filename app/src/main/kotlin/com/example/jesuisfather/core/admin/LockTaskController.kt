package com.example.jesuisfather.core.admin

import android.app.Activity
import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.os.Build

interface KioskPolicyManager {
    val isDeviceOwner: Boolean
    fun isLockTaskPermitted(packageName: String): Boolean
    fun setupKioskPolicies(whitelistedPackages: List<String>): Boolean
    fun startKiosk(activity: Activity): Boolean
    fun stopKiosk(activity: Activity): Boolean
    fun clearDeviceOwner(): Boolean
}

class LockTaskController(
    private val context: Context,
    private val dpm: DevicePolicyManager? = context.getSystemService(Context.DEVICE_POLICY_SERVICE) as? DevicePolicyManager
) : KioskPolicyManager {

    val adminComponent = ComponentName(context, JeSuisFatherAdminReceiver::class.java)

    override val isDeviceOwner: Boolean
        get() = dpm?.isDeviceOwnerApp(context.packageName) == true

    override fun isLockTaskPermitted(packageName: String): Boolean {
        return dpm?.isLockTaskPermitted(packageName) == true
    }

    override fun setupKioskPolicies(whitelistedPackages: List<String>): Boolean {
        val policyManager = dpm ?: return false
        if (!isDeviceOwner) return false

        val packagesToLock = (whitelistedPackages + context.packageName).distinct().toTypedArray()
        return try {
            policyManager.setLockTaskPackages(adminComponent, packagesToLock)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                // Disable status bar pull-down, quick settings tiles, and recents overview
                policyManager.setLockTaskFeatures(adminComponent, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
            }
            true
        } catch (_: SecurityException) {
            false
        }
    }

    override fun startKiosk(activity: Activity): Boolean {
        return try {
            activity.startLockTask()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun stopKiosk(activity: Activity): Boolean {
        return try {
            activity.stopLockTask()
            true
        } catch (_: Exception) {
            false
        }
    }

    override fun clearDeviceOwner(): Boolean {
        val policyManager = dpm ?: return false
        if (!isDeviceOwner) return false
        return try {
            policyManager.clearDeviceOwnerApp(context.packageName)
            true
        } catch (_: Exception) {
            false
        }
    }
}
