package com.example.soundpayapplication.util

import android.util.Log

data class PaymentInfo(
    val amount: String,
    val senderName: String?
)

object PaymentParser {
    private const val TAG = "PaymentParser"

    fun parsePaymentInfo(text: String, title: String, bigText: String): PaymentInfo? {
        try {
            val amount = extractAmount(text) ?: return null
            val senderName = extractSenderName(text, title, bigText)

            Log.d(TAG, "Parsed: Amount=$amount, Sender=$senderName")
            return PaymentInfo(amount, senderName)
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing payment info", e)
            return null
        }
    }

    private fun extractAmount(text: String): String? {
        // Pattern 1: ₹123 or ₹1,234.56
        val rupeePattern = "₹\\s*([0-9,]+\\.?[0-9]*)"
        var regex = Regex(rupeePattern)
        var match = regex.find(text)

        if (match != null) {
            val amount = match.groupValues[1].replace(",", "")
            return "₹$amount"
        }

        // Pattern 2: Rs 123 or Rs. 123
        val rsPattern = "(?:rs\\.?|inr)\\s*([0-9,]+\\.?[0-9]*)"
        regex = Regex(rsPattern, RegexOption.IGNORE_CASE)
        match = regex.find(text)

        if (match != null) {
            val amount = match.groupValues[1].replace(",", "")
            return "₹$amount"
        }

        // Pattern 3: 123.45 (standalone number with decimal)
        val numberPattern = "\\b([0-9,]+\\.[0-9]{2})\\b"
        regex = Regex(numberPattern)
        match = regex.find(text)

        if (match != null) {
            val amount = match.groupValues[1].replace(",", "")
            return "₹$amount"
        }

        return null
    }

    private fun extractSenderName(text: String, title: String, bigText: String): String? {
        // Common patterns for sender name
        val patterns = listOf(
            "from\\s+([A-Za-z][A-Za-z\\s]+?)(?:\\s+on|\\s+via|\\s+using|\\.|$)",
            "by\\s+([A-Za-z][A-Za-z\\s]+?)(?:\\s+on|\\s+via|\\s+using|\\.|$)",
            "payment\\s+from\\s+([A-Za-z][A-Za-z\\s]+?)(?:\\s+on|\\s+via|\\s+using|\\.|$)",
            "received\\s+from\\s+([A-Za-z][A-Za-z\\s]+?)(?:\\s+on|\\s+via|\\s+using|\\.|$)",
            "credited\\s+from\\s+([A-Za-z][A-Za-z\\s]+?)(?:\\s+on|\\s+via|\\s+using|\\.|$)",
            "paid\\s+by\\s+([A-Za-z][A-Za-z\\s]+?)(?:\\s+on|\\s+via|\\s+using|\\.|$)"
        )

        for (pattern in patterns) {
            val regex = Regex(pattern, RegexOption.IGNORE_CASE)
            val match = regex.find(text)
            if (match != null) {
                return match.groupValues[1].trim()
            }
        }

        // Try to extract from title if it contains a person's name
        if (title.isNotBlank() && !title.contains("payment", ignoreCase = true) &&
            !title.contains("received", ignoreCase = true) && title.length < 30) {
            // Check if title looks like a name (starts with capital, contains only letters and spaces)
            if (title.matches(Regex("^[A-Z][A-Za-z\\s]+$"))) {
                return title.trim()
            }
        }

        return null
    }
}

