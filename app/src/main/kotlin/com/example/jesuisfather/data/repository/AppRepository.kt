package com.example.jesuisfather.data.repository

import com.example.jesuisfather.data.local.AppWhitelistDao
import com.example.jesuisfather.data.model.AllowedApp
import com.example.jesuisfather.data.scanner.InstalledAppScanner
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface AppRepository {
    fun observeAllApps(): Flow<List<AllowedApp>>
    fun observeWhitelistedApps(): Flow<List<AllowedApp>>
    fun getAllowedApps(): List<AllowedApp>
    suspend fun refreshInstalledApps(): List<AllowedApp>
    suspend fun setAppEnabled(packageName: String, isEnabled: Boolean)
    suspend fun getTodayUsageSeconds(): Long
    suspend fun recordUsageSeconds(secondsToAdd: Long): Long
}

class DefaultAppRepository(
    private val dao: AppWhitelistDao,
    private val scanner: InstalledAppScanner? = null
) : AppRepository {

    override fun observeAllApps(): Flow<List<AllowedApp>> {
        return dao.observeAllApps()
    }

    override fun observeWhitelistedApps(): Flow<List<AllowedApp>> {
        return dao.observeAllowedApps()
    }

    override fun getAllowedApps(): List<AllowedApp> {
        return dao.getAllowedApps()
    }

    override suspend fun refreshInstalledApps(): List<AllowedApp> {
        if (scanner == null) {
            return dao.getAllApps()
        }
        val scanned = scanner.scanLaunchableApps()
        dao.upsertApps(scanned)
        return dao.getAllApps()
    }

    override suspend fun setAppEnabled(packageName: String, isEnabled: Boolean) {
        dao.setAppEnabled(packageName, isEnabled)
    }

    private fun getCurrentEpochDay(): Long {
        return try {
            LocalDate.now().toEpochDay()
        } catch (_: Throwable) {
            // Fallback for API < 26
            System.currentTimeMillis() / (1000 * 60 * 60 * 24)
        }
    }

    override suspend fun getTodayUsageSeconds(): Long {
        return dao.getUsageSecondsToday(getCurrentEpochDay())
    }

    override suspend fun recordUsageSeconds(secondsToAdd: Long): Long {
        return dao.addUsageSeconds(getCurrentEpochDay(), secondsToAdd)
    }
}
