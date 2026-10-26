package com.example.jesuisfather.core.admin

enum class ProtectionMode {
    STANDARD,      // Shared Daily-Driver Device: Home App Launcher Sandboxing
    ENTERPRISE     // Dedicated Child Device: ADB Device Owner & LockTask Hardware Kiosk
}
