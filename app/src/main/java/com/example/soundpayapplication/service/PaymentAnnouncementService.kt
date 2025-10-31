package com.example.soundpayapplication.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.soundpayapplication.MainActivity
import com.example.soundpayapplication.R
import com.example.soundpayapplication.util.PreferenceManager
import java.util.*

class PaymentAnnouncementService : Service(), TextToSpeech.OnInitListener {

    private val TAG = "PaymentAnnouncementService"
    private var tts: TextToSpeech? = null
    private var isTtsReady = false
    private lateinit var audioManager: AudioManager
    private var originalVolume = 0

    companion object {
        private const val CHANNEL_ID = "payment_announcement_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")

        tts = TextToSpeech(this, this)
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val amount = intent?.getStringExtra("amount") ?: ""
        val senderName = intent?.getStringExtra("senderName")

        Log.d(TAG, "Announcement request: amount=$amount, sender=$senderName")

        // Start foreground service
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)

        // Wait for TTS to be ready, then announce
        if (isTtsReady) {
            announcePayment(amount, senderName)
        } else {
            // Store for later announcement after TTS is initialized
            val pendingAnnouncement = PendingAnnouncement(amount, senderName)
            pendingAnnouncements.add(pendingAnnouncement)
        }

        return START_NOT_STICKY
    }

    private val pendingAnnouncements = mutableListOf<PendingAnnouncement>()

    data class PendingAnnouncement(val amount: String, val senderName: String?)

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val preferenceManager = PreferenceManager(this)
            val language = preferenceManager.getLanguage()

            val result = when (language) {
                "hindi" -> tts?.setLanguage(Locale.forLanguageTag("hi-IN"))
                else -> tts?.setLanguage(Locale.US)
            }

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "Language not supported, using default")
                tts?.setLanguage(Locale.US)
            }

            // Set speech rate and pitch
            tts?.setSpeechRate(0.9f)
            tts?.setPitch(1.0f)

            // Set utterance listener
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "TTS started")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "TTS completed")
                    stopSelf()
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "TTS error")
                    stopSelf()
                }
            })

            isTtsReady = true
            Log.d(TAG, "TTS initialized successfully")

            // Announce any pending payments
            if (pendingAnnouncements.isNotEmpty()) {
                pendingAnnouncements.forEach { pending ->
                    announcePayment(pending.amount, pending.senderName)
                }
                pendingAnnouncements.clear()
            }
        } else {
            Log.e(TAG, "TTS initialization failed")
            stopSelf()
        }
    }

    private fun announcePayment(amount: String, senderName: String?) {
        try {
            val preferenceManager = PreferenceManager(this)

            if (!preferenceManager.isAnnouncementEnabled()) {
                Log.d(TAG, "Announcements disabled in settings")
                stopSelf()
                return
            }

            // Boost volume temporarily if enabled
            if (preferenceManager.isVolumeBoostEnabled()) {
                boostVolume()
            }

            val message = createAnnouncementMessage(amount, senderName, preferenceManager.getLanguage())

            Log.d(TAG, "Announcing: $message")

            val params = Bundle()
            params.putString(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "payment_${System.currentTimeMillis()}")

            tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, "payment_${System.currentTimeMillis()}")

            // Restore volume after 3 seconds
            if (preferenceManager.isVolumeBoostEnabled()) {
                android.os.Handler(mainLooper).postDelayed({
                    restoreVolume()
                }, 3000)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error announcing payment", e)
            stopSelf()
        }
    }

    private fun createAnnouncementMessage(amount: String, senderName: String?, language: String): String {
        return when (language) {
            "hindi" -> {
                if (senderName != null) {
                    "Payment received $amount rupees $senderName se"
                } else {
                    "Payment received $amount rupees"
                }
            }
            else -> {
                if (senderName != null) {
                    "Payment received of $amount from $senderName"
                } else {
                    "Payment received of $amount"
                }
            }
        }
    }

    private fun boostVolume() {
        try {
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val targetVolume = (maxVolume * 0.8).toInt() // Set to 80% of max

            if (originalVolume < targetVolume) {
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, targetVolume, 0)
                Log.d(TAG, "Volume boosted from $originalVolume to $targetVolume")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error boosting volume", e)
        }
    }

    private fun restoreVolume() {
        try {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0)
            Log.d(TAG, "Volume restored to $originalVolume")
        } catch (e: Exception) {
            Log.e(TAG, "Error restoring volume", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Payment Announcements",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Announces incoming UPI payments"
                setShowBadge(false)
            }

            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): android.app.Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Payment Speaker Active")
            .setContentText("Listening for UPI payments")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        tts?.stop()
        tts?.shutdown()
        Log.d(TAG, "Service destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? = null
}

