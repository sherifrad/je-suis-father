package com.example.jesuisfather.data.scanner

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import com.example.jesuisfather.data.model.AllowedApp

class InstalledAppScanner(
    private val context: Context,
    private val packageManager: PackageManager = context.packageManager
) {

    companion object {
        // System and dangerous packages that should never be shown in a kids launcher
        val BLOCKED_PACKAGES = setOf(
            "com.android.settings",
            "com.android.packageinstaller",
            "com.google.android.packageinstaller",
            "com.android.vending", // Google Play Store
            "com.google.android.feedback",
            "com.android.systemui",
            "com.android.shell"
        )
    }

    fun scanLaunchableApps(): List<AllowedApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfoList = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            packageManager.queryIntentActivities(
                launcherIntent,
                PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_ALL.toLong())
            )
        } else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(launcherIntent, 0)
        }

        val selfPackage = context.packageName
        val scannedApps = mutableListOf<AllowedApp>()
        val seenPackages = mutableSetOf<String>()

        for (info in resolveInfoList) {
            val pkgName = info.activityInfo.packageName
            if (pkgName == selfPackage || pkgName in BLOCKED_PACKAGES || pkgName in seenPackages) {
                continue
            }

            val appName = try {
                info.loadLabel(packageManager).toString()
            } catch (_: Exception) {
                pkgName
            }

            seenPackages.add(pkgName)
            scannedApps.add(
                AllowedApp(
                    packageName = pkgName,
                    appName = appName,
                    isEnabled = false
                )
            )
        }

        return scannedApps.sortedBy { it.appName.lowercase() }
    }
}
