package com.example.jesuisfather.core.guard

import android.accessibilityservice.AccessibilityService
import android.app.ActivityOptions
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
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
            if (!className.contains("NavigationBar", ignoreCase = true) &&
                !className.contains("Keyguard", ignoreCase = true)
            ) {
                // Immediately collapse notification shade and panels
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE)
                }
                performGlobalAction(GLOBAL_ACTION_BACK)
            }
            return
        }

        // 2. Intercept Settings, App Store, and Package Installers
        if (BLOCKED_SYSTEM_PACKAGES.contains(packageName)) {
            pullLauncherToFront()
            return
        }

        // 3. Intercept unauthorized user applications (any app not explicitly whitelisted)
        val permitted = allowedPackages
        if (!permitted.contains(packageName)) {
            pullLauncherToFront()
        }
    }

    private fun pullLauncherToFront() {
        // 1. Immediately trigger HOME action at OS level to dismiss unauthorized window
        performGlobalAction(GLOBAL_ACTION_HOME)

        // 2. Also bring MainActivity directly to front
        try {
            val intent = Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_ACTIVITY_SINGLE_TOP or
                        Intent.FLAG_ACTIVITY_CLEAR_TOP
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val options = ActivityOptions.makeBasic().apply {
                    pendingIntentBackgroundActivityStartMode =
                        ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED
                }
                val pendingIntent = PendingIntent.getActivity(
                    applicationContext,
                    0,
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                    options.toBundle()
                )
                pendingIntent.send()
            } else {
                startActivity(intent)
            }
        } catch (_: Exception) {
            // Home action already invoked
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
