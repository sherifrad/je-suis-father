package com.example.jesuisfather.data.model

data class AllowedApp(
    val packageName: String,
    val appName: String,
    val isEnabled: Boolean = false,
    val maxDailyMinutes: Int = 0,
    val iconCachePath: String? = null
)
