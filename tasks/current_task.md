# Task Queue & Implementation Plan: Je Suis Father

**Project:** Je Suis Father (Open-Source Native Android Parental Control & Sandbox Launcher)  
**Status:** In Progress (Phase 1 & Phase 1.5 Specification & Visual Approval Gate)  
**Target:** Android 10+ (API 29–35), Kotlin, Jetpack Compose M3, Clean Architecture & Device Owner Kiosk  

---

## 📋 Task Backlog & Execution Milestones

### Phase 0 & 1: Specification, Architectural Audit & Compliance
- [x] **TASK-001**: Repository initialization, public GitHub remote setup (`sherifrad/je-suis-father`), and initial project skeleton (Completed 2026-10-26).
- [x] **TASK-002**: MACA v2 diagnostic preflight and Skill Ambassador ledger setup (`.agents/SKILL_COMPLIANCE_LOG.md`).
- [ ] **TASK-003**: In-depth Zero-Presumption Specification & Call-Chain Audit (`.agents/SPEC_AND_AUDIT.md`).
- [ ] **TASK-004**: Phase 1.5 Visual Mockup & Design Approval Gate (`.agents/mocks/je_suis_father_mock.html`).

### Phase 2: Security & Cryptographic Core (Milestone 1)
- [x] **TASK-101**: Update dependencies in `gradle/libs.versions.toml` & `app/build.gradle.kts` (Security Crypto, Biometric, Android 35 SDK alignment).
- [x] **TASK-102**: Implement `PinSecurityManager.kt` using `MasterKey` + PBKDF2WithHmacSHA256 (16-byte salt, 10k iterations, 5-attempt lockout).
- [x] **TASK-103**: Implement `BiometricAuthHelper.kt` with clean BiometricPrompt wrapper and PIN fallback.
- [x] **TASK-104**: Unit tests for PIN hashing, salt generation, verification, and brute-force lockout (`PinSecurityManagerTest.kt` passed all 6/6 tests in 0.19s).

### Phase 3: Data Layer & Application Package Discovery (Milestone 2)
- [x] **TASK-201**: Define Room/SQLite Database entities (`AllowedApp`, `UsageSession`) and `AppWhitelistDao`.
- [x] **TASK-202**: Implement `InstalledAppScanner.kt` to safely query launchable apps (`QUERY_ALL_PACKAGES` / `CATEGORY_LAUNCHER`), filtering system installers and settings.
- [x] **TASK-203**: Implement `AppRepository.kt` combining database states with installed system apps and daily usage accumulation.
- [x] **TASK-204**: Unit & deterministic tests for App Repository and Whitelist DAO (`AppRepositoryTest.kt` passed with 3/3 tests green).

### Phase 4: Kiosk Lockdown Engine & System Receivers (Milestone 3)
- [x] **TASK-301**: Create `res/xml/device_admin_sample.xml` and register `JeSuisFatherAdminReceiver` in `AndroidManifest.xml`.
- [x] **TASK-302**: Implement `LockTaskController.kt` for ADB Device Owner management (`setLockTaskPackages`, `LOCK_TASK_FEATURE_NONE`, `startLockTask`, `stopLockTask`).
- [x] **TASK-303**: Implement `BootReceiver.kt` for `ACTION_BOOT_COMPLETED` and `ACTION_LOCKED_BOOT_COMPLETED` auto-recovery.
- [x] **TASK-304**: Implement `ImmersiveModeHelper.kt` for system bar suppression on Honor MagicOS / Android devices.

### Phase 5: Dual-Persona Jetpack Compose UI (Milestone 4)
- [ ] **TASK-401**: Design theme tokens (`Theme.kt`, `Color.kt`, `Type.kt`) for Child Sandbox (Soft Sky Blue) and Parent Console (Dark Slate).
- [ ] **TASK-402**: Implement `OnboardingScreen.kt` (3-step HorizontalPager for PIN setup, whitelist curation, and launcher setup).
- [ ] **TASK-403**: Implement `KidsLauncherScreen.kt` (remaining time pill, battery gauge, 2-column oversized app grid, 2s lock press detector).
- [ ] **TASK-404**: Implement `PinAuthBottomSheet.kt` (4-dot PIN indicator, animated shake on error, 0-9 numeric pad, biometric trigger).
- [ ] **TASK-405**: Implement `ParentDashboardScreen.kt` (Device Owner status card, app search & switches, daily time slider, bedtime window, exit kiosk).

### Phase 6: Session Timers, Curfew Engine & Hardware Testing (Milestone 5)
- [ ] **TASK-501**: Implement `SessionTimerManager.kt` with monotonic elapsed-realtime tracking and persistent daily accumulation.
- [ ] **TASK-502**: Implement `TimeOutLockScreen.kt` (warm cream fullscreen non-dismissible lockout with Parent Unlock action).
- [ ] **TASK-503**: Implement `ParentIdleTimeoutTracker` (60s inactivity auto-relock into Kids Mode).
- [ ] **TASK-504**: Hardware and escape-vector verification on Honor MagicOS / AOSP.
