package com.example.jesuisfather.data.repository

import com.example.jesuisfather.data.local.AppWhitelistDao
import com.example.jesuisfather.data.model.AllowedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class AppRepositoryTest {

    private lateinit var fakeDao: FakeAppWhitelistDao
    private lateinit var repository: AppRepository

    @Before
    fun setup() {
        fakeDao = FakeAppWhitelistDao()
        repository = DefaultAppRepository(dao = fakeDao, scanner = null)
    }

    @Test
    fun observeWhitelistedApps_emitsOnlyEnabledApps() = runTest {
        fakeDao.upsertApp(AllowedApp("com.example.game", "Fun Game", isEnabled = true))
        fakeDao.upsertApp(AllowedApp("com.example.browser", "Web Browser", isEnabled = false))
        fakeDao.upsertApp(AllowedApp("com.example.learn", "Math Learn", isEnabled = true))

        val whitelisted = repository.observeWhitelistedApps().first()
        assertEquals(2, whitelisted.size)
        assertTrue(whitelisted.any { it.packageName == "com.example.game" })
        assertTrue(whitelisted.any { it.packageName == "com.example.learn" })
        assertFalse(whitelisted.any { it.packageName == "com.example.browser" })
    }

    @Test
    fun setAppEnabled_togglesAppStateInFlow() = runTest {
        fakeDao.upsertApp(AllowedApp("com.example.drawing", "Drawing Pad", isEnabled = false))

        var whitelisted = repository.observeWhitelistedApps().first()
        assertTrue(whitelisted.isEmpty())

        repository.setAppEnabled("com.example.drawing", true)
        whitelisted = repository.observeWhitelistedApps().first()
        assertEquals(1, whitelisted.size)
        assertEquals("com.example.drawing", whitelisted.first().packageName)

        repository.setAppEnabled("com.example.drawing", false)
        whitelisted = repository.observeWhitelistedApps().first()
        assertTrue(whitelisted.isEmpty())
    }

    @Test
    fun recordUsageSeconds_accumulatesPersistently() = runTest {
        assertEquals(0L, repository.getTodayUsageSeconds())

        repository.recordUsageSeconds(60L)
        assertEquals(60L, repository.getTodayUsageSeconds())

        repository.recordUsageSeconds(120L)
        assertEquals(180L, repository.getTodayUsageSeconds())
    }
}

/**
 * In-memory test double for AppWhitelistDao.
 */
class FakeAppWhitelistDao : AppWhitelistDao {
    private val appsMap = mutableMapOf<String, AllowedApp>()
    private val usageMap = mutableMapOf<Long, Long>()
    private val appsFlow = MutableStateFlow<List<AllowedApp>>(emptyList())

    private fun notifyFlow() {
        appsFlow.value = appsMap.values.toList()
    }

    override fun getAllApps(): List<AllowedApp> = appsMap.values.toList()

    override fun getAllowedApps(): List<AllowedApp> = appsMap.values.filter { it.isEnabled }

    override fun observeAllApps(): Flow<List<AllowedApp>> = appsFlow.asStateFlow()

    override fun observeAllowedApps(): Flow<List<AllowedApp>> =
        appsFlow.map { list -> list.filter { it.isEnabled } }

    override fun setAppEnabled(packageName: String, isEnabled: Boolean) {
        val app = appsMap[packageName]
        if (app != null) {
            appsMap[packageName] = app.copy(isEnabled = isEnabled)
            notifyFlow()
        }
    }

    override fun upsertApp(app: AllowedApp) {
        appsMap[app.packageName] = app
        notifyFlow()
    }

    override fun upsertApps(apps: List<AllowedApp>) {
        for (app in apps) {
            val existing = appsMap[app.packageName]
            appsMap[app.packageName] = app.copy(isEnabled = existing?.isEnabled ?: app.isEnabled)
        }
        notifyFlow()
    }

    override fun getUsageSecondsToday(dateEpochDay: Long): Long {
        return usageMap[dateEpochDay] ?: 0L
    }

    override fun addUsageSeconds(dateEpochDay: Long, secondsToAdd: Long): Long {
        val current = usageMap[dateEpochDay] ?: 0L
        val updated = current + secondsToAdd
        usageMap[dateEpochDay] = updated
        return updated
    }
}
