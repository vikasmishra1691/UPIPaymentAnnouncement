package com.example.soundpayapplication.service

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.soundpayapplication.data.PaymentDatabase
import com.example.soundpayapplication.data.PaymentEntity
import com.example.soundpayapplication.util.PaymentParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.jvm.java

class PaymentNotificationListener : NotificationListenerService() {

    private val TAG = "PaymentNotificationListener"
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val upiApps = listOf(
        "com.phonepe.app",
        "com.google.android.apps.nbu.paisa.user",
        "net.one97.paytm",
        "in.org.npci.upiapp",
        "in.amazon.mShop.android.shopping",
        "com.bharatpe.merchant.flutter",
        "com.freecharge.android",
        "com.mobikwik_new",
        "com.whatsapp",
        "com.google.android.gm" // Gmail for payment emails
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { statusBarNotification ->
            val packageName = statusBarNotification.packageName

            // Check if notification is from a UPI app
            if (packageName in upiApps) {
                processNotification(statusBarNotification)
            }
        }
    }

    private fun processNotification(sbn: StatusBarNotification) {
        try {
            val notification = sbn.notification ?: return
            val extras = notification.extras ?: return

            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: text

            val fullText = "$title $bigText".lowercase()

            Log.d(TAG, "Processing notification from ${sbn.packageName}: $fullText")

            // Check if this is a payment received notification
            if (isPaymentReceivedNotification(fullText)) {
                val paymentInfo = PaymentParser.parsePaymentInfo(fullText, title, bigText)

                if (paymentInfo != null) {
                    val appName = getAppName(sbn.packageName)

                    // Save to database
                    val paymentEntity = PaymentEntity(
                        amount = paymentInfo.amount,
                        senderName = paymentInfo.senderName,
                        appName = appName,
                        timestamp = System.currentTimeMillis(),
                        notificationText = fullText
                    )

                    serviceScope.launch {
                        val database = PaymentDatabase.getDatabase(applicationContext)
                        database.paymentDao().insert(paymentEntity)
                    }

                    // Trigger voice announcement
                    announcePayment(paymentInfo.amount, paymentInfo.senderName)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    private fun isPaymentReceivedNotification(text: String): Boolean {
        val keywords = listOf(
            "received",
            "credited",
            "payment received",
            "you received",
            "got ₹",
            "money received",
            "upi payment",
            "paid you"
        )

        return keywords.any { text.contains(it, ignoreCase = true) } &&
               (text.contains("₹") || text.contains("rs") || text.contains("inr"))
    }

    private fun announcePayment(amount: String, senderName: String?) {
        val intent = Intent(this, PaymentAnnouncementService::class.java).apply {
            putExtra("amount", amount)
            putExtra("senderName", senderName)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun getAppName(packageName: String): String {
        return when (packageName) {
            "com.phonepe.app" -> "PhonePe"
            "com.google.android.apps.nbu.paisa.user" -> "Google Pay"
            "net.one97.paytm" -> "Paytm"
            "in.org.npci.upiapp" -> "BHIM"
            "in.amazon.mShop.android.shopping" -> "Amazon Pay"
            "com.bharatpe.merchant.flutter" -> "BharatPe"
            "com.freecharge.android" -> "Freecharge"
            "com.mobikwik_new" -> "MobiKwik"
            "com.whatsapp" -> "WhatsApp"
            else -> "UPI App"
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.d(TAG, "Notification Listener Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.d(TAG, "Notification Listener Disconnected")
    }
}

