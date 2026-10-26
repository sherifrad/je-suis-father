package com.example.jesuisfather.core.security

import android.content.Context
import android.content.SharedPreferences
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.lang.reflect.Proxy

class PinSecurityManagerTest {

    private lateinit var fakePrefs: FakeSharedPreferences
    private lateinit var pinSecurityManager: PinSecurityManager

    @Before
    fun setup() {
        fakePrefs = FakeSharedPreferences()
        // Provide a dummy Context using dynamic proxy since testPrefs is supplied
        val dummyContext = Proxy.newProxyInstance(
            Context::class.java.classLoader,
            arrayOf(Context::class.java)
        ) { _, _, _ -> null } as Context

        pinSecurityManager = PinSecurityManager(dummyContext, fakePrefs)
    }

    @Test
    fun setPin_acceptsValidFourDigitPin() {
        assertTrue(pinSecurityManager.setPin("1234"))
        assertTrue(pinSecurityManager.isPinConfigured())
    }

    @Test
    fun setPin_rejectsInvalidLengthOrNonDigits() {
        assertFalse(pinSecurityManager.setPin("123"))
        assertFalse(pinSecurityManager.setPin("12345"))
        assertFalse(pinSecurityManager.setPin("12a4"))
        assertFalse(pinSecurityManager.isPinConfigured())
    }

    @Test
    fun verifyPin_succeedsWithCorrectPin() {
        pinSecurityManager.setPin("5678")
        val result = pinSecurityManager.verifyPin("5678")
        assertTrue(result is PinVerificationResult.Success)
    }

    @Test
    fun verifyPin_failsWithWrongPin_andDecrementsAttempts() {
        pinSecurityManager.setPin("5678")
        val result = pinSecurityManager.verifyPin("0000")
        assertTrue(result is PinVerificationResult.Failure)
        assertEquals(4, (result as PinVerificationResult.Failure).remainingAttempts)
    }

    @Test
    fun verifyPin_locksOutAfterFiveConsecutiveFailures() {
        pinSecurityManager.setPin("4321")
        val baseTime = 1_000_000L

        // 4 failed attempts
        for (i in 1..4) {
            val res = pinSecurityManager.verifyPin("0000", currentTimeMs = baseTime)
            assertTrue(res is PinVerificationResult.Failure)
            assertEquals(5 - i, (res as PinVerificationResult.Failure).remainingAttempts)
        }

        // 5th failed attempt -> Lockout triggered
        val fifthRes = pinSecurityManager.verifyPin("0000", currentTimeMs = baseTime)
        assertTrue(fifthRes is PinVerificationResult.LockedOut)
        assertEquals(300, (fifthRes as PinVerificationResult.LockedOut).remainingSeconds)

        // Attempt during lockout is blocked immediately even with correct PIN
        val lockedAttempt = pinSecurityManager.verifyPin("4321", currentTimeMs = baseTime + 60_000L)
        assertTrue(lockedAttempt is PinVerificationResult.LockedOut)
        assertEquals(240, (lockedAttempt as PinVerificationResult.LockedOut).remainingSeconds)

        // After 5 minutes, lockout expires and correct PIN succeeds
        val postLockout = pinSecurityManager.verifyPin("4321", currentTimeMs = baseTime + 301_000L)
        assertTrue(postLockout is PinVerificationResult.Success)
    }

    @Test
    fun resetFailedAttempts_clearsFailureCount() {
        pinSecurityManager.setPin("9999")
        pinSecurityManager.verifyPin("0000")
        pinSecurityManager.verifyPin("0000")
        assertEquals(2, fakePrefs.getInt(PinSecurityManager.KEY_FAILED_ATTEMPTS, 0))

        pinSecurityManager.resetFailedAttempts()
        assertEquals(0, fakePrefs.getInt(PinSecurityManager.KEY_FAILED_ATTEMPTS, 0))
    }
}

/**
 * Minimal in-memory SharedPreferences implementation for deterministic JVM unit tests.
 */
class FakeSharedPreferences : SharedPreferences {
    private val data = mutableMapOf<String, Any>()

    override fun getAll(): MutableMap<String, *> = data
    override fun getString(key: String?, defValue: String?): String? = data[key] as? String ?: defValue
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        @Suppress("UNCHECKED_CAST") (data[key] as? MutableSet<String> ?: defValues)
    override fun getInt(key: String?, defValue: Int): Int = data[key] as? Int ?: defValue
    override fun getLong(key: String?, defValue: Long): Long = data[key] as? Long ?: defValue
    override fun getFloat(key: String?, defValue: Float): Float = data[key] as? Float ?: defValue
    override fun getBoolean(key: String?, defValue: Boolean): Boolean = data[key] as? Boolean ?: defValue
    override fun contains(key: String?): Boolean = data.containsKey(key)
    override fun edit(): SharedPreferences.Editor = FakeEditor(data)
    override fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}
    override fun unregisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener?) {}

    class FakeEditor(private val data: MutableMap<String, Any>) : SharedPreferences.Editor {
        private val temp = mutableMapOf<String, Any?>()
        private var clearRequested = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            if (key != null) temp[key] = values
            return this
        }
        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            if (key != null) temp[key] = value
            return this
        }
        override fun remove(key: String?): SharedPreferences.Editor {
            if (key != null) temp[key] = null
            return this
        }
        override fun clear(): SharedPreferences.Editor {
            clearRequested = true
            return this
        }
        override fun commit(): Boolean {
            apply()
            return true
        }
        override fun apply() {
            if (clearRequested) data.clear()
            for ((k, v) in temp) {
                if (v == null) data.remove(k) else data[k] = v
            }
        }
    }
}
