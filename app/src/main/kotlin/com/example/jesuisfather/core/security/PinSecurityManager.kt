package com.example.jesuisfather.core.security

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.jesuisfather.core.admin.ProtectionMode
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult
    data class Failure(val remainingAttempts: Int) : PinVerificationResult
    data class LockedOut(val remainingSeconds: Int) : PinVerificationResult
}

class PinSecurityManager(
    context: Context? = null,
    testPrefs: SharedPreferences? = null
) {
    private val prefs: SharedPreferences = testPrefs ?: createEncryptedPreferences(
        checkNotNull(context) { "Context must be provided when testPrefs is null" }
    )

    companion object {
        const val PREFS_FILE_NAME = "jesuisfather_secure_prefs"
        const val KEY_PIN_HASH = "pin_hash"
        const val KEY_PIN_SALT = "pin_salt"
        const val KEY_FAILED_ATTEMPTS = "failed_attempts"
        const val KEY_LOCKOUT_UNTIL_MS = "lockout_until_ms"
        const val KEY_PROTECTION_MODE = "protection_mode"
        const val KEY_DAILY_LIMIT_MINUTES = "daily_limit_minutes"
        const val KEY_BEDTIME_CURFEW = "bedtime_curfew_enabled"
        const val DEFAULT_DAILY_LIMIT_MINUTES = 90

        const val MAX_FAILED_ATTEMPTS = 5
        const val LOCKOUT_DURATION_MS = 5 * 60 * 1000L // 5 minutes
        private const val PBKDF2_ITERATIONS = 10_000
        private const val PBKDF2_KEY_LENGTH = 256
        private const val SALT_SIZE_BYTES = 16

        private fun createEncryptedPreferences(context: Context): SharedPreferences {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            return EncryptedSharedPreferences.create(
                context,
                PREFS_FILE_NAME,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }

        fun hashPin(pin: CharArray, salt: ByteArray): ByteArray {
            val spec = PBEKeySpec(pin, salt, PBKDF2_ITERATIONS, PBKDF2_KEY_LENGTH)
            return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        }

        fun encodeBase64(bytes: ByteArray): String {
            return try {
                java.util.Base64.getEncoder().encodeToString(bytes)
            } catch (_: Throwable) {
                android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
            }
        }

        fun decodeBase64(str: String): ByteArray {
            return try {
                java.util.Base64.getDecoder().decode(str)
            } catch (_: Throwable) {
                android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
            }
        }
    }

    fun isPinConfigured(): Boolean {
        return prefs.getString(KEY_PIN_HASH, null) != null
    }

    fun setPin(pin: String): Boolean {
        if (pin.length != 4 || !pin.all { it.isDigit() }) {
            return false
        }
        val salt = ByteArray(SALT_SIZE_BYTES).apply { SecureRandom().nextBytes(this) }
        val hash = hashPin(pin.toCharArray(), salt)

        prefs.edit()
            .putString(KEY_PIN_HASH, encodeBase64(hash))
            .putString(KEY_PIN_SALT, encodeBase64(salt))
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
            .apply()
        return true
    }

    fun getLockoutRemainingSeconds(currentTimeMs: Long = System.currentTimeMillis()): Int {
        val lockoutUntil = prefs.getLong(KEY_LOCKOUT_UNTIL_MS, 0L)
        val remainingMs = lockoutUntil - currentTimeMs
        return if (remainingMs > 0) ((remainingMs + 999) / 1000).toInt() else 0
    }

    fun verifyPin(
        enteredPin: String,
        currentTimeMs: Long = System.currentTimeMillis()
    ): PinVerificationResult {
        val lockoutRemainingSec = getLockoutRemainingSeconds(currentTimeMs)
        if (lockoutRemainingSec > 0) {
            return PinVerificationResult.LockedOut(lockoutRemainingSec)
        }

        val storedHash = prefs.getString(KEY_PIN_HASH, null) ?: return PinVerificationResult.Failure(0)
        val storedSalt = prefs.getString(KEY_PIN_SALT, null) ?: return PinVerificationResult.Failure(0)

        val saltBytes = decodeBase64(storedSalt)
        val computedHash = hashPin(enteredPin.toCharArray(), saltBytes)
        val computedHashStr = encodeBase64(computedHash)

        return if (storedHash == computedHashStr) {
            resetFailedAttempts()
            PinVerificationResult.Success
        } else {
            val currentAttempts = prefs.getInt(KEY_FAILED_ATTEMPTS, 0) + 1
            if (currentAttempts >= MAX_FAILED_ATTEMPTS) {
                val lockoutUntil = currentTimeMs + LOCKOUT_DURATION_MS
                prefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, currentAttempts)
                    .putLong(KEY_LOCKOUT_UNTIL_MS, lockoutUntil)
                    .apply()
                PinVerificationResult.LockedOut((LOCKOUT_DURATION_MS / 1000).toInt())
            } else {
                prefs.edit()
                    .putInt(KEY_FAILED_ATTEMPTS, currentAttempts)
                    .apply()
                PinVerificationResult.Failure(MAX_FAILED_ATTEMPTS - currentAttempts)
            }
        }
    }

    fun resetFailedAttempts() {
        prefs.edit()
            .putInt(KEY_FAILED_ATTEMPTS, 0)
            .putLong(KEY_LOCKOUT_UNTIL_MS, 0L)
            .apply()
    }

    fun getProtectionMode(): ProtectionMode {
        val modeStr = prefs.getString(KEY_PROTECTION_MODE, ProtectionMode.STANDARD.name)
        return try {
            ProtectionMode.valueOf(modeStr ?: ProtectionMode.STANDARD.name)
        } catch (_: Exception) {
            ProtectionMode.STANDARD
        }
    }

    fun setProtectionMode(mode: ProtectionMode) {
        prefs.edit()
            .putString(KEY_PROTECTION_MODE, mode.name)
            .apply()
    }

    fun getDailyLimitMinutes(): Int {
        return prefs.getInt(KEY_DAILY_LIMIT_MINUTES, DEFAULT_DAILY_LIMIT_MINUTES)
    }

    fun setDailyLimitMinutes(minutes: Int) {
        prefs.edit()
            .putInt(KEY_DAILY_LIMIT_MINUTES, minutes)
            .apply()
    }

    fun isBedtimeCurfewEnabled(): Boolean {
        return prefs.getBoolean(KEY_BEDTIME_CURFEW, false)
    }

    fun setBedtimeCurfewEnabled(enabled: Boolean) {
        prefs.edit()
            .putBoolean(KEY_BEDTIME_CURFEW, enabled)
            .apply()
    }
}
