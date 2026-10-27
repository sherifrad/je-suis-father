package com.example.jesuisfather.core.timer

import android.os.SystemClock
import com.example.jesuisfather.data.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalTime

class SessionTimerManager(
    private val repository: AppRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private var dailyLimitMinutes: Int = 90,
    initialCurfewEnabled: Boolean = false
) {
    private val _remainingMinutes = MutableStateFlow(dailyLimitMinutes)
    val remainingMinutes: StateFlow<Int> = _remainingMinutes.asStateFlow()

    private val _isLockedOut = MutableStateFlow(false)
    val isLockedOut: StateFlow<Boolean> = _isLockedOut.asStateFlow()

    private var bonusMinutes = 0
    private var tickerJob: Job? = null
    private var lastTickRealtime = 0L
    private var isBedtimeCurfewEnabled = initialCurfewEnabled

    fun setDailyLimitMinutes(minutes: Int) {
        dailyLimitMinutes = minutes
        updateRemaining()
    }

    fun getDailyLimitMinutes(): Int = dailyLimitMinutes

    fun setBedtimeCurfewEnabled(enabled: Boolean) {
        isBedtimeCurfewEnabled = enabled
        updateRemaining()
    }

    fun isBedtimeCurfewEnabled(): Boolean = isBedtimeCurfewEnabled

    fun grantBonusMinutes(minutes: Int) {
        bonusMinutes += minutes
        updateRemaining()
    }

    fun resetDailyUsage() {
        bonusMinutes = 0
        scope.launch {
            updateRemaining()
        }
    }

    fun startTimer() {
        if (tickerJob?.isActive == true) return
        lastTickRealtime = SystemClock.elapsedRealtime()

        tickerJob = scope.launch {
            while (isActive) {
                delay(1000L)
                val now = SystemClock.elapsedRealtime()
                val elapsedSeconds = (now - lastTickRealtime) / 1000L
                if (elapsedSeconds >= 1L) {
                    lastTickRealtime = now
                    repository.recordUsageSeconds(elapsedSeconds)
                    updateRemaining()
                }
            }
        }
    }

    fun pauseTimer() {
        tickerJob?.cancel()
        tickerJob = null
    }

    private fun updateRemaining() {
        scope.launch {
            val usedSeconds = repository.getTodayUsageSeconds()
            val totalAllowedMinutes = dailyLimitMinutes + bonusMinutes
            val remainingSec = (totalAllowedMinutes * 60L) - usedSeconds

            val isBedtime = checkBedtime()
            if (isBedtime || remainingSec <= 0) {
                _remainingMinutes.value = 0
                _isLockedOut.value = true
            } else {
                _remainingMinutes.value = ((remainingSec + 59) / 60).toInt()
                _isLockedOut.value = false
            }
        }
    }

    private fun checkBedtime(): Boolean {
        if (!isBedtimeCurfewEnabled) return false
        return try {
            val now = LocalTime.now()
            // Bedtime: 20:00 to 07:00
            val hour = now.hour
            hour >= 20 || hour < 7
        } catch (_: Throwable) {
            false
        }
    }
}
