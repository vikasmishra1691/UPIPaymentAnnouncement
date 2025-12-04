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
        Log.d(TAG, "Extracting amount from: $text")

        // Pattern 1: ₹123 or ₹1,234.56 or ₹1.00 or ₹1
        val rupeePattern = "₹\\s*([0-9,]+\\.?[0-9]*)"
        var regex = Regex(rupeePattern)
        var match = regex.find(text)

        if (match != null) {
            val rawAmount = match.groupValues[1].replace(",", "")
            val amount = formatAmount(rawAmount)
            Log.d(TAG, "Found amount with ₹: $amount")
            return amount
        }

        // Pattern 2: Rs 123 or Rs. 123 or Rs.1
        val rsPattern = "(?:rs\\.?|inr)\\s*([0-9,]+\\.?[0-9]*)"
        regex = Regex(rsPattern, RegexOption.IGNORE_CASE)
        match = regex.find(text)

        if (match != null) {
            val rawAmount = match.groupValues[1].replace(",", "")
            val amount = formatAmount(rawAmount)
            Log.d(TAG, "Found amount with Rs: $amount")
            return amount
        }

        // Pattern 3: Any number with decimal (e.g., 1.00, 123.45)
        val decimalPattern = "\\b([0-9,]+\\.[0-9]+)\\b"
        regex = Regex(decimalPattern)
        match = regex.find(text)

        if (match != null) {
            val rawAmount = match.groupValues[1].replace(",", "")
            val amount = formatAmount(rawAmount)
            Log.d(TAG, "Found amount with decimal: $amount")
            return amount
        }

        // Pattern 4: Standalone whole number near payment keywords
        val numberPattern = "\\b([1-9][0-9]{0,7})\\b"
        regex = Regex(numberPattern)
        val matches = regex.findAll(text).toList()

        if (matches.isNotEmpty()) {
            // Take the first number found (likely the amount)
            val rawAmount = matches[0].groupValues[1]
            val amount = formatAmount(rawAmount)
            Log.d(TAG, "Found standalone number: $amount")
            return amount
        }

        Log.w(TAG, "Could not extract amount from text")
        return null
    }

    /**
     * Format amount to ensure consistent storage format: ₹123.45
     * This ensures database queries work correctly
     */
    private fun formatAmount(rawAmount: String): String {
        val amount = rawAmount.toDoubleOrNull() ?: return "₹0.00"

        // If the amount has no decimal part or is a whole number, add .00
        return if (rawAmount.contains(".")) {
            "₹$rawAmount"
        } else {
            "₹$rawAmount.00"
        }
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

