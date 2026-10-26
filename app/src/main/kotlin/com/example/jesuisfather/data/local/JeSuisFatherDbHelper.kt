package com.example.jesuisfather.data.local

import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.jesuisfather.data.model.AllowedApp
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

class JeSuisFatherDbHelper(
    context: Context,
    dbName: String = DATABASE_NAME
) : SQLiteOpenHelper(context, dbName, null, DATABASE_VERSION), AppWhitelistDao {

    companion object {
        const val DATABASE_NAME = "jesuisfather.db"
        const val DATABASE_VERSION = 1

        const val TABLE_ALLOWED_APPS = "allowed_apps"
        const val COL_PACKAGE_NAME = "package_name"
        const val COL_APP_NAME = "app_name"
        const val COL_IS_ENABLED = "is_enabled"
        const val COL_MAX_DAILY_MINUTES = "max_daily_minutes"
        const val COL_ICON_CACHE_PATH = "icon_cache_path"

        const val TABLE_USAGE_SESSIONS = "usage_sessions"
        const val COL_ID = "id"
        const val COL_DATE_EPOCH_DAY = "date_epoch_day"
        const val COL_DURATION_SECONDS = "duration_seconds"

        const val TABLE_DAILY_USAGE = "daily_usage"
        const val COL_ACCUMULATED_SECONDS = "accumulated_seconds"
    }

    private val allAppsStateFlow = MutableStateFlow<List<AllowedApp>>(emptyList())

    init {
        // Initial load of apps cache
        refreshCache()
    }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_ALLOWED_APPS (
                $COL_PACKAGE_NAME TEXT PRIMARY KEY,
                $COL_APP_NAME TEXT NOT NULL,
                $COL_IS_ENABLED INTEGER NOT NULL DEFAULT 0,
                $COL_MAX_DAILY_MINUTES INTEGER NOT NULL DEFAULT 0,
                $COL_ICON_CACHE_PATH TEXT
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_USAGE_SESSIONS (
                $COL_ID INTEGER PRIMARY KEY AUTOINCREMENT,
                $COL_PACKAGE_NAME TEXT NOT NULL,
                $COL_DATE_EPOCH_DAY INTEGER NOT NULL,
                $COL_DURATION_SECONDS INTEGER NOT NULL
            )
            """.trimIndent()
        )

        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS $TABLE_DAILY_USAGE (
                $COL_DATE_EPOCH_DAY INTEGER PRIMARY KEY,
                $COL_ACCUMULATED_SECONDS INTEGER NOT NULL DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Future migrations will be additive to respect SaMD Zero-Delete
    }

    private fun refreshCache() {
        allAppsStateFlow.value = queryAllAppsFromDb()
    }

    private fun queryAllAppsFromDb(): List<AllowedApp> {
        val result = mutableListOf<AllowedApp>()
        val db = readableDatabase
        val cursor: Cursor = db.query(
            TABLE_ALLOWED_APPS,
            null,
            null,
            null,
            null,
            null,
            "$COL_APP_NAME COLLATE NOCASE ASC"
        )
        cursor.use {
            val pkgIdx = it.getColumnIndexOrThrow(COL_PACKAGE_NAME)
            val nameIdx = it.getColumnIndexOrThrow(COL_APP_NAME)
            val enabledIdx = it.getColumnIndexOrThrow(COL_IS_ENABLED)
            val limitIdx = it.getColumnIndexOrThrow(COL_MAX_DAILY_MINUTES)
            val iconIdx = it.getColumnIndexOrThrow(COL_ICON_CACHE_PATH)

            while (it.moveToNext()) {
                result.add(
                    AllowedApp(
                        packageName = it.getString(pkgIdx),
                        appName = it.getString(nameIdx),
                        isEnabled = it.getInt(enabledIdx) == 1,
                        maxDailyMinutes = it.getInt(limitIdx),
                        iconCachePath = it.getString(iconIdx)
                    )
                )
            }
        }
        return result
    }

    override fun getAllApps(): List<AllowedApp> {
        return queryAllAppsFromDb()
    }

    override fun getAllowedApps(): List<AllowedApp> {
        return queryAllAppsFromDb().filter { it.isEnabled }
    }

    override fun observeAllApps(): Flow<List<AllowedApp>> {
        return allAppsStateFlow.asStateFlow()
    }

    override fun observeAllowedApps(): Flow<List<AllowedApp>> {
        return allAppsStateFlow.map { list -> list.filter { it.isEnabled } }
    }

    override fun setAppEnabled(packageName: String, isEnabled: Boolean) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_IS_ENABLED, if (isEnabled) 1 else 0)
        }
        db.update(
            TABLE_ALLOWED_APPS,
            values,
            "$COL_PACKAGE_NAME = ?",
            arrayOf(packageName)
        )
        refreshCache()
    }

    override fun upsertApp(app: AllowedApp) {
        val db = writableDatabase
        val values = ContentValues().apply {
            put(COL_PACKAGE_NAME, app.packageName)
            put(COL_APP_NAME, app.appName)
            put(COL_IS_ENABLED, if (app.isEnabled) 1 else 0)
            put(COL_MAX_DAILY_MINUTES, app.maxDailyMinutes)
            put(COL_ICON_CACHE_PATH, app.iconCachePath)
        }
        db.insertWithOnConflict(
            TABLE_ALLOWED_APPS,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        refreshCache()
    }

    override fun upsertApps(apps: List<AllowedApp>) {
        val db = writableDatabase
        db.beginTransaction()
        try {
            for (app in apps) {
                // Preserve is_enabled if already in db
                val existing = findApp(db, app.packageName)
                val isEnabled = existing?.isEnabled ?: app.isEnabled
                val values = ContentValues().apply {
                    put(COL_PACKAGE_NAME, app.packageName)
                    put(COL_APP_NAME, app.appName)
                    put(COL_IS_ENABLED, if (isEnabled) 1 else 0)
                    put(COL_MAX_DAILY_MINUTES, app.maxDailyMinutes)
                    put(COL_ICON_CACHE_PATH, app.iconCachePath)
                }
                db.insertWithOnConflict(
                    TABLE_ALLOWED_APPS,
                    null,
                    values,
                    SQLiteDatabase.CONFLICT_REPLACE
                )
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshCache()
    }

    private fun findApp(db: SQLiteDatabase, packageName: String): AllowedApp? {
        val cursor = db.query(
            TABLE_ALLOWED_APPS,
            arrayOf(COL_IS_ENABLED),
            "$COL_PACKAGE_NAME = ?",
            arrayOf(packageName),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                val enabled = it.getInt(0) == 1
                return AllowedApp(packageName = packageName, appName = "", isEnabled = enabled)
            }
        }
        return null
    }

    override fun getUsageSecondsToday(dateEpochDay: Long): Long {
        val db = readableDatabase
        val cursor = db.query(
            TABLE_DAILY_USAGE,
            arrayOf(COL_ACCUMULATED_SECONDS),
            "$COL_DATE_EPOCH_DAY = ?",
            arrayOf(dateEpochDay.toString()),
            null,
            null,
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                return it.getLong(0)
            }
        }
        return 0L
    }

    override fun addUsageSeconds(dateEpochDay: Long, secondsToAdd: Long): Long {
        val db = writableDatabase
        val current = getUsageSecondsToday(dateEpochDay)
        val newTotal = current + secondsToAdd
        val values = ContentValues().apply {
            put(COL_DATE_EPOCH_DAY, dateEpochDay)
            put(COL_ACCUMULATED_SECONDS, newTotal)
        }
        db.insertWithOnConflict(
            TABLE_DAILY_USAGE,
            null,
            values,
            SQLiteDatabase.CONFLICT_REPLACE
        )
        return newTotal
    }
}
