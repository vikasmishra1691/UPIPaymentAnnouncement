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
        "com.phonepe.app",                              // PhonePe
        "com.google.android.apps.nbu.paisa.user",      // Google Pay
        "net.one97.paytm",                             // Paytm
        "in.org.npci.upiapp",                          // BHIM UPI
        "in.amazon.mShop.android.shopping",            // Amazon Pay
        "com.bharatpe.merchant.flutter",               // BharatPe
        "com.freecharge.android",                      // Freecharge
        "com.mobikwik_new",                            // MobiKwik
        "com.whatsapp",                                // WhatsApp Pay
        "com.paypal.android.p2pmobile",                // PayPal
        "com.dreamplug.androidapp",                    // CRED
        "com.myairtelapp",                             // Airtel Payments Bank
        "com.csam.icici.bank.imobile",                 // iMobile Pay
        "com.axis.mobile",                             // Axis Mobile
        "com.sbi.lotusintouch",                        // YONO SBI
        "com.snapwork.hdfc",                           // HDFC Bank
        "com.fb.app",                                  // Federal Bank
        "com.pnb.onlite"                              // PNB One
    )

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)

        sbn?.let { statusBarNotification ->
            val packageName = statusBarNotification.packageName

            // Check if notification is from a UPI app
            if (packageName in upiApps) {
                Log.d(TAG, "Notification from UPI app: $packageName")
                processNotification(statusBarNotification)
            } else {
                Log.v(TAG, "Ignoring notification from non-UPI app: $packageName")
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
                Log.d(TAG, "✓ Detected as payment received notification")
                val paymentInfo = PaymentParser.parsePaymentInfo(fullText, title, bigText)

                if (paymentInfo != null) {
                    Log.d(TAG, "✓ Parsed payment: ${paymentInfo.amount} from ${paymentInfo.senderName}")
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
                } else {
                    Log.w(TAG, "✗ Could not parse payment info from notification")
                }
            } else {
                Log.d(TAG, "✗ Not a payment received notification (might be sent payment or other)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification", e)
        }
    }

    private fun isPaymentReceivedNotification(text: String): Boolean {
        // Keywords that indicate payment received (not sent)
        val receivedKeywords = listOf(
            "received",
            "credited",
            "payment received",
            "you received",
            "got ₹",
            "money received",
            "upi payment received",
            "paid you",
            "credit",
            "deposited"
        )

        // Keywords that indicate payment sent (should be excluded)
        val sentKeywords = listOf(
            "sent",
            "paid to",
            "payment to",
            "debited",
            "debit",
            "transferred to",
            "paid ₹"
        )

        // Must contain currency symbol or keyword
        val hasCurrency = text.contains("₹") ||
                         text.contains("rs.", ignoreCase = true) ||
                         text.contains("inr", ignoreCase = true) ||
                         text.contains("rupees", ignoreCase = true)

        // Must contain received keyword and NOT contain sent keyword
        val hasReceivedKeyword = receivedKeywords.any { text.contains(it, ignoreCase = true) }
        val hasSentKeyword = sentKeywords.any { text.contains(it, ignoreCase = true) }

        // Must be from UPI context
        val isUpiContext = text.contains("upi", ignoreCase = true) ||
                          text.contains("payment", ignoreCase = true) ||
                          text.contains("transaction", ignoreCase = true)

        return hasCurrency && hasReceivedKeyword && !hasSentKeyword && isUpiContext
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
            "com.paypal.android.p2pmobile" -> "PayPal"
            "com.dreamplug.androidapp" -> "CRED"
            "com.myairtelapp" -> "Airtel Payments"
            "com.csam.icici.bank.imobile" -> "iMobile Pay"
            "com.axis.mobile" -> "Axis Mobile"
            "com.sbi.lotusintouch" -> "YONO SBI"
            "com.snapwork.hdfc" -> "HDFC Bank"
            "com.fb.app" -> "Federal Bank"
            "com.pnb.onlite" -> "PNB One"
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

