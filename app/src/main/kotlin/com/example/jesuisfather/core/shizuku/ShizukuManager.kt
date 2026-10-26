package com.example.jesuisfather.core.shizuku

import android.content.ComponentName
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.io.BufferedReader
import java.io.InputStreamReader

data class ShizukuCommandResult(
    val exitCode: Int,
    val output: String,
    val error: String,
    val isSuccess: Boolean = exitCode == 0
)

object ShizukuManager {

    fun isShizukuAvailable(): Boolean {
        return try {
            Shizuku.pingBinder()
        } catch (_: Throwable) {
            false
        }
    }

    fun isPermissionGranted(): Boolean {
        if (!isShizukuAvailable()) return false
        return try {
            if (Shizuku.isPreV11()) {
                false
            } else {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            }
        } catch (_: Throwable) {
            false
        }
    }

    fun requestPermission(requestCode: Int = 1001): Boolean {
        if (!isShizukuAvailable()) return false
        return try {
            Shizuku.requestPermission(requestCode)
            true
        } catch (_: Throwable) {
            false
        }
    }

    suspend fun executeShellCommand(command: String): ShizukuCommandResult = withContext(Dispatchers.IO) {
        if (!isPermissionGranted()) {
            return@withContext ShizukuCommandResult(
                exitCode = -1,
                output = "",
                error = "Shizuku permission not granted or service inactive."
            )
        }

        try {
            val process = Shizuku.newProcess(arrayOf("sh", "-c", command), null, null)
            val output = BufferedReader(InputStreamReader(process.inputStream)).use { it.readText() }.trim()
            val error = BufferedReader(InputStreamReader(process.errorStream)).use { it.readText() }.trim()
            val exitCode = process.waitFor()

            ShizukuCommandResult(
                exitCode = exitCode,
                output = output,
                error = error,
                isSuccess = exitCode == 0
            )
        } catch (e: Throwable) {
            ShizukuCommandResult(
                exitCode = -1,
                output = "",
                error = e.localizedMessage ?: "Unknown execution error"
            )
        }
    }

    suspend fun setDeviceOwner(adminComponent: ComponentName): ShizukuCommandResult {
        val flattened = adminComponent.flattenToString()
        return executeShellCommand("dpm set-device-owner $flattened")
    }
}
