package com.example.jesuisfather.core.guard

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import com.example.jesuisfather.MainActivity

class JeSuisFatherAccessibilityService : AccessibilityService() {

    companion object {
        @Volatile
        var isKidsModeActive: Boolean = false

        @Volatile
        var allowedPackages: Set<String> = emptySet()

        @Volatile
        var isServiceRunning: Boolean = false

        private val SYSTEM_UI_PACKAGES = setOf(
            "com.android.systemui",
            "com.hihonor.systemui",
            "com.huawei.systemui"
        )

        private val BLOCKED_SYSTEM_PACKAGES = setOf(
            "com.android.settings",
            "com.hihonor.settings",
            "com.huawei.android.launcher",
            "com.hihonor.android.launcher",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.vending"
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        isServiceRunning = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isKidsModeActive || event == null) return

        val packageName = event.packageName?.toString() ?: return
        if (packageName == applicationContext.packageName) return

        // 1. Intercept Notification Shade / Quick Settings pull-downs
        if (SYSTEM_UI_PACKAGES.contains(packageName)) {
            val className = event.className?.toString().orEmpty()
            if (className.contains("Notification", ignoreCase = true) ||
                className.contains("Panel", ignoreCase = true) ||
                className.contains("Shade", ignoreCase = true) ||
                className.contains("QuickSettings", ignoreCase = true)
            ) {
                // Immediately collapse by executing BACK global action
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            return
        }

        // 2. Intercept Settings, App Store, and Package Installers
        if (BLOCKED_SYSTEM_PACKAGES.contains(packageName)) {
            pullLauncherToFront()
            return
        }

        // 3. Intercept unauthorized user applications
        val permitted = allowedPackages
        if (permitted.isNotEmpty() && !permitted.contains(packageName)) {
            pullLauncherToFront()
        }
    }

    private fun pullLauncherToFront() {
        try {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            startActivity(intent)
        } catch (_: Exception) {
            performGlobalAction(GLOBAL_ACTION_HOME)
        }
    }

    override fun onInterrupt() {
        isServiceRunning = false
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceRunning = false
    }
}
