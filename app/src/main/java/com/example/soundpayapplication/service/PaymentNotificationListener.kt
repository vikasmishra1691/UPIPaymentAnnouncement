package com.example.soundpayapplication.service

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.widget.Toast
import com.example.soundpayapplication.data.PaymentDatabase
import com.example.soundpayapplication.data.PaymentEntity
import com.example.soundpayapplication.util.PaymentParser
import com.example.soundpayapplication.util.ISTTimeHelper
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

            // Log ALL notifications for debugging
            Log.d(TAG, "========================================")
            Log.d(TAG, "Notification received from: $packageName")

            // Check if notification is from a UPI app
            if (packageName in upiApps) {
                Log.d(TAG, "✓ UPI app detected: $packageName")
                processNotification(statusBarNotification)
            } else {
                Log.v(TAG, "✗ Ignoring non-UPI app: $packageName")
            }
            Log.d(TAG, "========================================")
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

            Log.d(TAG, "--- Notification Details ---")
            Log.d(TAG, "Package: ${sbn.packageName}")
            Log.d(TAG, "Title: $title")
            Log.d(TAG, "Text: $text")
            Log.d(TAG, "BigText: $bigText")
            Log.d(TAG, "Full text (lowercase): $fullText")

            // Check if this is a payment received notification
            if (isPaymentReceivedNotification(fullText)) {
                Log.d(TAG, "✓ Detected as PAYMENT RECEIVED notification")

                // Show toast for debugging
                android.os.Handler(android.os.Looper.getMainLooper()).post {
                    Toast.makeText(
                        applicationContext,
                        "Payment notification detected!",
                        Toast.LENGTH_SHORT
                    ).show()
                }

                val paymentInfo = PaymentParser.parsePaymentInfo(fullText, title, bigText)

                if (paymentInfo != null) {
                    Log.d(TAG, "✓ Parsed payment successfully: ${paymentInfo.amount} from ${paymentInfo.senderName}")

                    // Show toast with payment details
                    android.os.Handler(android.os.Looper.getMainLooper()).post {
                        Toast.makeText(
                            applicationContext,
                            "Payment: ${paymentInfo.amount} from ${paymentInfo.senderName ?: "Unknown"}",
                            Toast.LENGTH_LONG
                        ).show()
                    }

                    val appName = getAppName(sbn.packageName)

                    val currentTimestamp = ISTTimeHelper.getCurrentTimeMillis()
                    val istTime = ISTTimeHelper.formatTimestamp(currentTimestamp)
                    Log.d(TAG, "Payment timestamp: $currentTimestamp ($istTime)")

                    // Save to database
                    val paymentEntity = PaymentEntity(
                        amount = paymentInfo.amount,
                        senderName = paymentInfo.senderName,
                        appName = appName,
                        timestamp = currentTimestamp,
                        notificationText = fullText
                    )

                    serviceScope.launch {
                        try {
                            val database = PaymentDatabase.getDatabase(applicationContext)
                            database.paymentDao().insert(paymentEntity)
                            Log.d(TAG, "✓ Payment saved to database")
                            Log.d(TAG, "  Amount: ${paymentEntity.amount}")
                            Log.d(TAG, "  Sender: ${paymentEntity.senderName}")
                            Log.d(TAG, "  App: ${paymentEntity.appName}")
                            Log.d(TAG, "  Timestamp: ${paymentEntity.timestamp} ($istTime)")

                            // Verify the save by checking database count
                            val allPayments = database.paymentDao().getAllPayments()
                            Log.d(TAG, "  Database should now have updated payment count")

                        } catch (e: Exception) {
                            Log.e(TAG, "✗ Error saving to database", e)
                        }
                    }

                    // Trigger voice announcement
                    Log.d(TAG, "✓ Triggering voice announcement")
                    announcePayment(paymentInfo.amount, paymentInfo.senderName)
                } else {
                    Log.w(TAG, "✗ Could not parse payment info from notification text")
                }
            } else {
                Log.d(TAG, "✗ Not a payment received notification (might be sent payment or other type)")
            }
        } catch (e: Exception) {
            Log.e(TAG, "✗ Error processing notification", e)
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
            "deposited",
            "incoming",
            "money added",
            "balance updated"
        )

        // Keywords that indicate payment sent (should be excluded)
        val sentKeywords = listOf(
            "sent",
            "paid to",
            "payment to",
            "debited",
            "debit",
            "transferred to",
            "paid ₹",
            "payment made",
            "successfully paid"
        )

        // Must contain currency symbol or keyword
        val hasCurrency = text.contains("₹") ||
                         text.contains("rs.", ignoreCase = true) ||
                         text.contains("rs ", ignoreCase = true) ||
                         text.contains("inr", ignoreCase = true) ||
                         text.contains("rupees", ignoreCase = true)

        // Must contain received keyword and NOT contain sent keyword
        val hasReceivedKeyword = receivedKeywords.any { text.contains(it, ignoreCase = true) }
        val hasSentKeyword = sentKeywords.any { text.contains(it, ignoreCase = true) }

        // Must be from UPI/payment context (more lenient)
        val isUpiContext = text.contains("upi", ignoreCase = true) ||
                          text.contains("payment", ignoreCase = true) ||
                          text.contains("transaction", ignoreCase = true) ||
                          text.contains("money", ignoreCase = true) ||
                          text.contains("amount", ignoreCase = true)

        Log.d(TAG, "Payment Detection:")
        Log.d(TAG, "  - Has currency: $hasCurrency")
        Log.d(TAG, "  - Has received keyword: $hasReceivedKeyword")
        Log.d(TAG, "  - Has sent keyword: $hasSentKeyword")
        Log.d(TAG, "  - Is UPI context: $isUpiContext")

        val isReceived = hasCurrency && hasReceivedKeyword && !hasSentKeyword && isUpiContext
        Log.d(TAG, "  - Final result: ${if (isReceived) "PAYMENT RECEIVED ✓" else "NOT payment received ✗"}")

        return isReceived
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

