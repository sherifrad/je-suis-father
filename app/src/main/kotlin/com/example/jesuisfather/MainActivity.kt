package com.example.jesuisfather

import android.content.Intent
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.jesuisfather.core.admin.LockTaskController
import com.example.jesuisfather.core.admin.ProtectionMode
import com.example.jesuisfather.core.security.BiometricAuthHelper
import com.example.jesuisfather.core.security.PinSecurityManager
import com.example.jesuisfather.core.security.PinVerificationResult
import com.example.jesuisfather.core.timer.SessionTimerManager
import com.example.jesuisfather.core.ui.ImmersiveModeHelper
import com.example.jesuisfather.data.local.JeSuisFatherDbHelper
import com.example.jesuisfather.data.model.AllowedApp
import com.example.jesuisfather.data.repository.DefaultAppRepository
import com.example.jesuisfather.data.scanner.InstalledAppScanner
import com.example.jesuisfather.ui.components.PinAuthBottomSheet
import com.example.jesuisfather.ui.kids.KidsLauncherScreen
import com.example.jesuisfather.ui.lockout.TimeOutLockScreen
import com.example.jesuisfather.ui.onboarding.OnboardingScreen
import com.example.jesuisfather.ui.parent.ParentDashboardScreen
import com.example.jesuisfather.ui.theme.JeSuisFatherTheme
import kotlinx.coroutines.launch

enum class ScreenState {
    ONBOARDING,
    KIDS_SANDBOX,
    PARENT_DASHBOARD,
    LOCKOUT
}

class MainActivity : FragmentActivity() {

    private lateinit var pinManager: PinSecurityManager
    private lateinit var dbHelper: JeSuisFatherDbHelper
    private lateinit var appRepository: DefaultAppRepository
    private lateinit var lockTaskController: LockTaskController
    private lateinit var sessionTimer: SessionTimerManager
    private lateinit var biometricHelper: BiometricAuthHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pinManager = PinSecurityManager(context = this)
        dbHelper = JeSuisFatherDbHelper(context = this)
        val scanner = InstalledAppScanner(context = this)
        appRepository = DefaultAppRepository(dao = dbHelper, scanner = scanner)
        lockTaskController = LockTaskController(context = this)
        sessionTimer = SessionTimerManager(repository = appRepository)

        setContent {
            JeSuisFatherTheme {
                val scope = rememberCoroutineScope()
                var currentScreen by remember {
                    mutableStateOf(
                        if (pinManager.isPinConfigured()) ScreenState.KIDS_SANDBOX else ScreenState.ONBOARDING
                    )
                }

                var showPinModal by remember { mutableStateOf(false) }
                var pinErrorMessage by remember { mutableStateOf<String?>(null) }
                var lockoutSeconds by remember { mutableIntStateOf(0) }

                var currentProtectionMode by remember {
                    mutableStateOf(pinManager.getProtectionMode())
                }
                var isDeviceOwnerState by remember {
                    mutableStateOf(lockTaskController.isDeviceOwner)
                }

                val allApps by appRepository.observeAllApps().collectAsState(initial = emptyList())
                val whitelistedApps by appRepository.observeWhitelistedApps().collectAsState(initial = emptyList())
                val remainingMinutes by sessionTimer.remainingMinutes.collectAsState()
                val isLockedOut by sessionTimer.isLockedOut.collectAsState()

                // Register biometric helper
                biometricHelper = remember {
                    BiometricAuthHelper(
                        activity = this@MainActivity,
                        onAuthSuccess = {
                            showPinModal = false
                            pinErrorMessage = null
                            if (currentScreen == ScreenState.LOCKOUT) {
                                sessionTimer.grantBonusMinutes(30)
                                currentScreen = ScreenState.KIDS_SANDBOX
                            } else {
                                currentScreen = ScreenState.PARENT_DASHBOARD
                            }
                        },
                        onAuthError = { _, errString ->
                            pinErrorMessage = errString.toString()
                        },
                        onAuthFailed = {
                            pinErrorMessage = "Biometric not recognized. Use PIN."
                        }
                    )
                }

                // Check lockout status
                LaunchedEffect(isLockedOut) {
                    if (isLockedOut && currentScreen == ScreenState.KIDS_SANDBOX) {
                        currentScreen = ScreenState.LOCKOUT
                    }
                }

                // Start or pause session timer based on screen
                LaunchedEffect(currentScreen) {
                    if (currentScreen == ScreenState.KIDS_SANDBOX) {
                        sessionTimer.startTimer()
                        ImmersiveModeHelper.enterImmersiveMode(window)
                        val packages = whitelistedApps.map { it.packageName }
                        if (currentProtectionMode == ProtectionMode.ENTERPRISE && lockTaskController.isDeviceOwner) {
                            lockTaskController.setupKioskPolicies(packages)
                        }
                        lockTaskController.startKiosk(this@MainActivity)
                    } else {
                        sessionTimer.pauseTimer()
                        ImmersiveModeHelper.exitImmersiveMode(window)
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (currentScreen) {
                        ScreenState.ONBOARDING -> {
                            LaunchedEffect(Unit) {
                                appRepository.refreshInstalledApps()
                            }
                            OnboardingScreen(
                                installedApps = allApps,
                                isDeviceOwner = lockTaskController.isDeviceOwner,
                                onSavePin = { pin -> pinManager.setPin(pin) },
                                onCompleteOnboarding = { selectedPackages ->
                                    scope.launch {
                                        for (pkg in selectedPackages) {
                                            appRepository.setAppEnabled(pkg, true)
                                        }
                                        currentScreen = ScreenState.KIDS_SANDBOX
                                    }
                                },
                                onOpenHomeSettings = {
                                    startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                }
                            )
                        }

                        ScreenState.KIDS_SANDBOX -> {
                            KidsLauncherScreen(
                                whitelistedApps = whitelistedApps,
                                remainingMinutes = remainingMinutes,
                                onAppClick = { packageName ->
                                    val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
                                    if (launchIntent != null) {
                                        startActivity(launchIntent)
                                    } else {
                                        Toast.makeText(this@MainActivity, "Unable to launch app", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onParentLockLongPress = {
                                    pinErrorMessage = null
                                    lockoutSeconds = pinManager.getLockoutRemainingSeconds()
                                    showPinModal = true
                                }
                            )
                        }

                        ScreenState.PARENT_DASHBOARD -> {
                            ParentDashboardScreen(
                                isDeviceOwner = isDeviceOwnerState,
                                allApps = allApps,
                                dailyLimitMinutes = 90,
                                protectionMode = currentProtectionMode,
                                onProtectionModeChanged = { newMode ->
                                    currentProtectionMode = newMode
                                    pinManager.setProtectionMode(newMode)
                                },
                                onCheckDeviceOwnerStatus = {
                                    isDeviceOwnerState = lockTaskController.isDeviceOwner
                                },
                                onToggleApp = { packageName, isEnabled ->
                                    scope.launch {
                                        appRepository.setAppEnabled(packageName, isEnabled)
                                    }
                                },
                                onDailyLimitChanged = { minutes ->
                                    sessionTimer.setDailyLimitMinutes(minutes)
                                },
                                onLockAndReturn = {
                                    currentScreen = ScreenState.KIDS_SANDBOX
                                },
                                onExitKiosk = {
                                    lockTaskController.stopKiosk(this@MainActivity)
                                    startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
                                }
                            )
                        }

                        ScreenState.LOCKOUT -> {
                            TimeOutLockScreen(
                                onParentUnlockClick = {
                                    pinErrorMessage = null
                                    lockoutSeconds = pinManager.getLockoutRemainingSeconds()
                                    showPinModal = true
                                }
                            )
                        }
                    }

                    // Parental PIN Authentication Modal
                    if (showPinModal) {
                        PinAuthBottomSheet(
                            remainingLockoutSeconds = lockoutSeconds,
                            errorMessage = pinErrorMessage,
                            onPinEntered = { enteredPin ->
                                when (val result = pinManager.verifyPin(enteredPin)) {
                                    is PinVerificationResult.Success -> {
                                        showPinModal = false
                                        pinErrorMessage = null
                                        if (currentScreen == ScreenState.LOCKOUT) {
                                            sessionTimer.grantBonusMinutes(30)
                                            currentScreen = ScreenState.KIDS_SANDBOX
                                        } else {
                                            currentScreen = ScreenState.PARENT_DASHBOARD
                                        }
                                    }
                                    is PinVerificationResult.Failure -> {
                                        pinErrorMessage = "Incorrect PIN (${result.remainingAttempts} attempts remaining)"
                                    }
                                    is PinVerificationResult.LockedOut -> {
                                        lockoutSeconds = result.remainingSeconds
                                        pinErrorMessage = "Keypad locked due to too many failed attempts"
                                    }
                                }
                            },
                            onBiometricClick = {
                                if (biometricHelper.isBiometricAvailable(this@MainActivity)) {
                                    biometricHelper.promptBiometric()
                                } else {
                                    pinErrorMessage = "Biometrics not available on this device"
                                }
                            },
                            onDismiss = {
                                showPinModal = false
                                pinErrorMessage = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Ensure default launcher state and refresh apps
        if (::appRepository.isInitialized) {
            val scope = kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO)
            scope.launch {
                appRepository.refreshInstalledApps()
            }
        }
    }
}
