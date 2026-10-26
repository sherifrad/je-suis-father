package com.example.jesuisfather.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.jesuisfather.MainActivity
import com.example.jesuisfather.core.security.PinSecurityManager

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_LOCKED_BOOT_COMPLETED) {
            val pinManager = PinSecurityManager(context)
            if (pinManager.isPinConfigured()) {
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                }
                context.startActivity(launchIntent)
            }
        }
    }
}
