package com.example.soundpayapplication.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.soundpayapplication.util.PreferenceManager

class BootReceiver : BroadcastReceiver() {

    private val TAG = "BootReceiver"

    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            Log.d(TAG, "Boot completed, checking if auto-start is enabled")

            context?.let {
                val preferenceManager = PreferenceManager(it)

                if (preferenceManager.isAutoStartEnabled()) {
                    Log.d(TAG, "Auto-start enabled, notification listener should start automatically")
                    // The NotificationListenerService will start automatically if enabled
                }
            }
        }
    }
}

