package com.example.jesuisfather.data.model

data class UsageSession(
    val id: Long = 0,
    val packageName: String,
    val dateEpochDay: Long,
    val durationSeconds: Long
)
