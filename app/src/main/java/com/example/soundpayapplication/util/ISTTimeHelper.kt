package com.example.soundpayapplication.util

import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Helper class for working with Indian Standard Time (IST)
 * All time calculations should use this helper to ensure consistency
 */
object ISTTimeHelper {

    private const val IST_TIMEZONE_ID = "Asia/Kolkata"

    /**
     * Get the IST TimeZone
     */
    fun getISTTimeZone(): TimeZone {
        return TimeZone.getTimeZone(IST_TIMEZONE_ID)
    }

    /**
     * Get current time in milliseconds (IST)
     */
    fun getCurrentTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    /**
     * Get Calendar instance with IST timezone
     */
    fun getISTCalendar(): Calendar {
        return Calendar.getInstance(getISTTimeZone())
    }

    /**
     * Get timestamp for start of today (12:00 AM) in IST
     */
    fun getTodayStartTimeMillis(): Long {
        val calendar = getISTCalendar()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Get timestamp for start of current week (Sunday 12:00 AM) in IST
     */
    fun getWeekStartTimeMillis(): Long {
        val calendar = getISTCalendar()

        // Set to start of today
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)

        // Get current day of week (1 = Sunday, 7 = Saturday)
        val dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK)

        // Calculate days to subtract to get to Sunday
        val daysToSubtract = dayOfWeek - Calendar.SUNDAY

        // Go back to Sunday
        calendar.add(Calendar.DAY_OF_YEAR, -daysToSubtract)

        return calendar.timeInMillis
    }

    /**
     * Get timestamp for start of current month (1st day 12:00 AM) in IST
     */
    fun getMonthStartTimeMillis(): Long {
        val calendar = getISTCalendar()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    /**
     * Format timestamp to human readable IST date/time
     */
    fun formatTimestamp(timestamp: Long): String {
        val calendar = getISTCalendar()
        calendar.timeInMillis = timestamp

        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1 // 0-indexed
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        return String.format(Locale.US, "%02d/%02d/%04d %02d:%02d IST", day, month, year, hour, minute)
    }

    /**
     * Check if a timestamp is from today (IST)
     */
    fun isToday(timestamp: Long): Boolean {
        return timestamp >= getTodayStartTimeMillis()
    }

    /**
     * Check if a timestamp is from current week (IST)
     */
    fun isThisWeek(timestamp: Long): Boolean {
        return timestamp >= getWeekStartTimeMillis()
    }

    /**
     * Check if a timestamp is from current month (IST)
     */
    fun isThisMonth(timestamp: Long): Boolean {
        return timestamp >= getMonthStartTimeMillis()
    }
}

