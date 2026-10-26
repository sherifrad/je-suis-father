# Specification & Zero-Presumption Audit (SPEC_AND_AUDIT.md)

## 1. Executive Summary & Problem Statement
* **Objective**: Build **Je Suis Father**, a tamper-resistant, offline-first native Android parental control and sandboxed kids launcher. The application transforms standard Android smartphones (specifically optimized for Honor MagicOS / EMUI and AOSP) into an impenetrable, child-friendly environment.
* **Scope**:
  - In-Scope: Custom Home launcher (`CATEGORY_HOME`), package whitelist discovery & curation, ADB Device Owner LockTask kiosk engine (`LOCK_TASK_FEATURE_NONE`), PBKDF2 salted PIN security + Biometrics, persistent daily screen time & bedtime curfew governance, Dual-Persona Jetpack Compose M3 UI.
  - Out-of-Scope: Cloud synchronization, remote telemetry, third-party analytics (strictly offline-first and private).
* **Active Domain Pack**: Systems / Core Engineering & Child Safety Hardening
* **Linked Task Queue Item**: `TASK-003` in `tasks/current_task.md`

---

## 2. Zero-Presumption Architectural & Call-Chain Audit
*Rigorous audit of the incoming specifications and existing codebase.*

### Discovered Defects & Fragilities in Source Specifications
1. **Reboot Screen Time Bypass (Critical)**:
   - *Source Fragility*: The initial specification described in-memory session countdown. A child could restart the device to reset the session timer and regain 45 minutes of playtime.
   - *Remediation*: Screen time accumulation must be stored in Room DB / persistent preferences per calendar day (`dateEpochDay` + `accumulatedSeconds`). On boot, remaining time is strictly `max(0, maxDailyMinutes * 60 - accumulatedSeconds)`.
2. **Deprecated Android Keystore APIs**:
   - *Source Fragility*: Use of deprecated `androidx.security.crypto.MasterKeys.getOrCreate()`.
   - *Remediation*: Migrate to modern `androidx.security.crypto.MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()`.
3. **Emergency Call Lockout Legal Risk**:
   - *Source Fragility*: `LOCK_TASK_FEATURE_NONE` completely suppresses the emergency dialer dialpad on standard lock screens.
   - *Remediation*: Provide a dedicated, non-lockout emergency SOS dialer fallback or allow system emergency intents (`ACTION_DIAL` for 911 / 112 / local emergency) to ensure safety compliance.
4. **Android 14+ / 15+ Receiver Export & Foreground Rules**:
   - *Source Fragility*: Unspecified export flags on dynamic receivers and outdated Compose compiler version flags.
   - *Remediation*: Enforce explicit `RECEIVER_NOT_EXPORTED` on internal broadcasts and use Kotlin 2.0+ official Compose Compiler plugin.
5. **Honor MagicOS Gesture Navigation vs Immersive Window**:
   - *Source Fragility*: Curved edge swipe gestures on devices like the Honor 400 can trigger system back gesture if not scoped.
   - *Remediation*: Set `LOCK_TASK_FEATURE_NONE` via Device Owner, disabling system gesture interception and navigation bar bypass completely.

### Cross-Function Call-Chain Map
| Target Function / Method | Upstream Callers (UI / ViewModels / Receivers) | Downstream Callees (DAOs / Security / OS DPM) | Parameter & Unit Contract Risks |
| :--- | :--- | :--- | :--- |
| `PinSecurityManager.verifyPin()` | `PinAuthBottomSheet`, `OnboardingScreen` | `SecretKeyFactory`, `EncryptedSharedPreferences` | Enforce 4-digit numeric string; verify non-null salt and PBKDF2 10k iterations. |
| `LockTaskController.setupKioskPolicies()` | `KidsLauncherActivity.onResume()`, `ParentDashboardViewModel` | `DevicePolicyManager.setLockTaskPackages()`, `setLockTaskFeatures()` | Must pass non-empty package array containing host package (`context.packageName`) + whitelisted apps. |
| `SessionTimerManager.tick()` | `KidsLauncherViewModel`, `ForegroundService` | `AppUsageDao.addUsageSeconds()`, `TimeOutLockScreen` | Use monotonic `SystemClock.elapsedRealtime()` to prevent wall-clock tampering. |
| `InstalledAppScanner.query()` | `OnboardingViewModel`, `ParentDashboardViewModel` | `PackageManager.queryIntentActivities()` | Filter out system settings (`com.android.settings`), app installers, package installers, and host app. |

---

## 3. The Target Specification

### 3.1 Component & Interface Contracts

```kotlin
// Security Contract
interface PinSecurityService {
    fun isPinConfigured(): Boolean
    fun setPin(pin: String): Boolean
    fun verifyPin(enteredPin: String): PinVerificationResult
    fun getLockoutRemainingSeconds(): Int
    fun resetFailedAttempts()
}

sealed interface PinVerificationResult {
    data object Success : PinVerificationResult
    data class Failure(val remainingAttempts: Int) : PinVerificationResult
    data class LockedOut(val remainingSeconds: Int) : PinVerificationResult
}

// Kiosk & Device Policy Contract
interface KioskController {
    val isDeviceOwner: Boolean
    val isLockTaskActive: Boolean
    fun updateWhitelistedPackages(packages: List<String>)
    fun startKiosk(activity: Activity)
    fun stopKiosk(activity: Activity)
}

// Session & Time Governance Contract
interface SessionGovernanceManager {
    val remainingTimeSeconds: StateFlow<Long>
    val isBedtimeActive: StateFlow<Boolean>
    val isLockedOut: StateFlow<Boolean>
    fun recordForegroundTick(elapsedSeconds: Long)
    fun grantBonusMinutes(minutes: Int)
}
```

### 3.2 Dual-Persona Design Tokens & Color Systems
* **Child Persona**:
  - Surface: `Soft Sky Blue` (`#E3F2FD`)
  - Accent Cards: `Games Blue` (`#5C9CE6`), `Learn Amber` (`#FFD54F`), `Draw Green` (`#66BB6A`), `Music Pink` (`#EC407A`), `Camera Purple` (`#AB47BC`), `Books Orange` (`#FFA726`)
  - Target Dimensions: Oversized 96dp touch targets, 140dp card height, 24dp corner radii.
* **Parent Persona**:
  - Surface: `Deep Slate` (`#12161F`)
  - Elevated Card: `Elevated Slate` (`#1E2430`)
  - Status Indicator: `Emerald Green` (`#4CAF50`) for Device Owner Active; `Amber` (`#FFB300`) for Standard Pinning.
* **Lockout Persona**:
  - Surface: `Warm Cream` (`#FFF8E7`)
  - Unlock Action: `Soft Coral Pill` (`#E57373`)

### 3.3 Visual Mockup & UX Contract (Phase 1.5 Approval Gate)
* **Design Lead**: System Architect / Generative UI
* **Mock Artifact Path**: [`.agents/mocks/je_suis_father_mock.html`](file:///Users/sherifgamalradwan/Je%20suis%20Father/.agents/mocks/je_suis_father_mock.html)
* **States Covered**:
  1. Empty Whitelist State (`لا توجد تطبيقات مصرح بها` / `No whitelisted apps`)
  2. Loading / Scanning State (`Loading installed apps...` / Shimmer)
  3. Active Kiosk Launcher State with Time Remaining Pill & Battery
  4. Error / PIN Shake & Brute-Force Lockout Countdown State
  5. Bilingual (LTR English & RTL Arabic) with Narrow 360dp Phone and 840dp Tablet Viewports

---

## 4. Phase 4 Dedicated E2E & Sequential Scenario Matrix

| Scenario ID | Sequential Stimulus ($T_0 \to T_{+1} \to T_{+2}$) | Call-Chain Path Exercised | Logical / Domain Oracle | Persistence (DB) Oracle | Visual (UI) Oracle |
| :--- | :--- | :--- | :--- | :--- | :--- |
| `E2E-PIN-01` | Enter wrong PIN 5 consecutive times. | `PinAuthBottomSheet` $\rightarrow$ `PinSecurityManager.verifyPin()` | Emits `LockedOut(300)` on 5th failure; suppresses keypad inputs. | Lockout expiration timestamp written to `EncryptedSharedPreferences`. | Keypad disabled; shake animation triggered; "Try again in 5:00" displayed in red. |
| `E2E-TIME-01` | Active session reaches 00:00 while child is inside a game. | `SessionTimerManager` $\rightarrow$ `KidsLauncherActivity` foreground intent | Emits `isLockedOut = true`; brings `TimeOutLockScreen` to front with `FLAG_ACTIVITY_NEW_TASK`. | `accumulatedSeconds` saved to Room DB; persisted across reboots. | Non-dismissible warm cream lockout screen overlays game; back button consumed. |
| `E2E-REBOOT-01` | Child hard reboots the phone during an active session. | `BootReceiver` $\rightarrow$ `LockTaskController.startKiosk()` | `ACTION_BOOT_COMPLETED` triggered; verifies default launcher; re-arms LockTask immediately. | Rehydrates remaining daily allowance from Room DB without resetting to max. | Device boots directly into Kids Home Launcher or Lockout screen without exposing system launcher. |
| `NEG-CTRL-01` | Child taps corner Lock button once. | `KidsLauncherScreen` pointer input | Single tap is consumed with no auth dialog shown. | Database and security state unchanged. | Subtle tooltip shown: "Hold for 2 seconds for Parent Mode"; PIN sheet remains closed. |
