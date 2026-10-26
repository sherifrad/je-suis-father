package com.example.jesuisfather.data.local

import com.example.jesuisfather.data.model.AllowedApp
import kotlinx.coroutines.flow.Flow

interface AppWhitelistDao {
    fun getAllApps(): List<AllowedApp>
    fun getAllowedApps(): List<AllowedApp>
    fun observeAllApps(): Flow<List<AllowedApp>>
    fun observeAllowedApps(): Flow<List<AllowedApp>>
    fun setAppEnabled(packageName: String, isEnabled: Boolean)
    fun upsertApp(app: AllowedApp)
    fun upsertApps(apps: List<AllowedApp>)
    fun getUsageSecondsToday(dateEpochDay: Long): Long
    fun addUsageSeconds(dateEpochDay: Long, secondsToAdd: Long): Long
}
