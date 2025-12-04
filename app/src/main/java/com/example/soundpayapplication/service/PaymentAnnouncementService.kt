package com.example.soundpayapplication.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFocusRequest
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

    companion object {
        private const val CHANNEL_ID = "payment_announcement_channel"
        private const val NOTIFICATION_ID = 1
    }

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "========================================")
        Log.d(TAG, "★ PAYMENT ANNOUNCEMENT SERVICE CREATED ★")
        Log.d(TAG, "========================================")

        Log.d(TAG, "→ Initializing TextToSpeech...")
        tts = TextToSpeech(this, this)

        Log.d(TAG, "→ Getting AudioManager...")
        audioManager = getSystemService(AUDIO_SERVICE) as AudioManager

        Log.d(TAG, "→ Creating notification channel...")
        createNotificationChannel()

        Log.d(TAG, "✓ Service creation complete")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val amount = intent?.getStringExtra("amount") ?: ""
        val senderName = intent?.getStringExtra("senderName")

        Log.d(TAG, "========================================")
        Log.d(TAG, "★ ANNOUNCEMENT SERVICE STARTED ★")
        Log.d(TAG, "Amount: $amount")
        Log.d(TAG, "Sender: ${senderName ?: "Unknown"}")
        Log.d(TAG, "TTS Ready: $isTtsReady")
        Log.d(TAG, "========================================")

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
        Log.d(TAG, "→ onInit() called with status: $status")

        if (status == TextToSpeech.SUCCESS) {
            Log.d(TAG, "✓ TTS initialization SUCCESS")

            val preferenceManager = PreferenceManager(this)
            val language = preferenceManager.getLanguage()

            Log.d(TAG, "→ Setting language: $language")

            val result = when (language) {
                "hindi" -> tts?.setLanguage(Locale.forLanguageTag("hi-IN"))
                else -> tts?.setLanguage(Locale.US)
            }

            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "✗ Language not supported, using default")
                tts?.setLanguage(Locale.US)
            } else {
                Log.d(TAG, "✓ Language set successfully")
            }

            // Set speech rate and pitch
            tts?.setSpeechRate(0.9f)
            tts?.setPitch(1.0f)
            Log.d(TAG, "✓ Speech rate and pitch configured")

            // Set utterance listener
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    Log.d(TAG, "★ TTS STARTED speaking: $utteranceId")
                }

                override fun onDone(utteranceId: String?) {
                    Log.d(TAG, "★ TTS COMPLETED speaking: $utteranceId")
                    stopSelf()
                }

                override fun onError(utteranceId: String?) {
                    Log.e(TAG, "✗ TTS ERROR for utterance: $utteranceId")
                    stopSelf()
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?, errorCode: Int) {
                    Log.e(TAG, "✗ TTS ERROR for utterance: $utteranceId, code: $errorCode")
                    stopSelf()
                }
            })

            isTtsReady = true
            Log.d(TAG, "✓✓✓ TTS is now READY ✓✓✓")

            // Announce any pending payments
            if (pendingAnnouncements.isNotEmpty()) {
                Log.d(TAG, "→ Processing ${pendingAnnouncements.size} pending announcements")
                pendingAnnouncements.forEach { pending ->
                    announcePayment(pending.amount, pending.senderName)
                }
                pendingAnnouncements.clear()
            } else {
                Log.d(TAG, "→ No pending announcements")
            }
        } else {
            Log.e(TAG, "✗✗✗ TTS initialization FAILED with status: $status")
            stopSelf()
        }
    }

    private fun announcePayment(amount: String, senderName: String?) {
        try {
            Log.d(TAG, "→ announcePayment() called")
            Log.d(TAG, "  Amount: $amount")
            Log.d(TAG, "  Sender: ${senderName ?: "None"}")

            val preferenceManager = PreferenceManager(this)

            if (!preferenceManager.isAnnouncementEnabled()) {
                Log.w(TAG, "✗ Announcements disabled in settings - stopping service")
                stopSelf()
                return
            }

            Log.d(TAG, "✓ Announcements enabled in settings")

            if (!isTtsReady || tts == null) {
                Log.e(TAG, "✗ TTS not ready - stopping service")
                stopSelf()
                return
            }

            Log.d(TAG, "✓ TTS is ready")

            val message = createAnnouncementMessage(amount, senderName, preferenceManager.getLanguage())
            val utteranceId = "payment_${System.currentTimeMillis()}"

            Log.d(TAG, "→ Message to announce: \"$message\"")
            Log.d(TAG, "→ Utterance ID: $utteranceId")

            // Configure audio for announcement
            val params = Bundle()
            params.putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)

            // Request audio focus
            Log.d(TAG, "→ Requesting audio focus...")
            requestAudioFocus()

            // Speak using phone audio
            Log.d(TAG, "→ Calling TTS speak()...")
            Log.d(TAG, "★★★ ANNOUNCING: $message ★★★")
            val speakResult = tts?.speak(message, TextToSpeech.QUEUE_FLUSH, params, utteranceId)
            Log.d(TAG, "→ TTS speak() returned: $speakResult")

            if (speakResult == TextToSpeech.ERROR) {
                Log.e(TAG, "✗ TTS speak() returned ERROR")
            } else {
                Log.d(TAG, "✓ TTS speak() call successful")
            }

        } catch (e: Exception) {
            Log.e(TAG, "✗ Exception in announcePayment", e)
            stopSelf()
        }
    }

    private fun requestAudioFocus() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()

                val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .build()

                audioManager.requestAudioFocus(focusRequest)
            } else {
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    null,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting audio focus", e)
        }
    }



    private fun createAnnouncementMessage(amount: String, senderName: String?, language: String): String {
        return when (language) {
            "hindi" -> {
                if (senderName != null) {
                    "$amount रुपये $senderName से प्राप्त हुए"
                } else {
                    "$amount रुपये प्राप्त हुए"
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

