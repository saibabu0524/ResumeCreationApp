package com.softsuave.resumecreationapp.core.common.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Utility class for date and time operations
 */
object DateTimeUtil {

    private const val ISO_8601_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"
    private const val DISPLAY_DATE_PATTERN = "MMM dd, yyyy"
    private const val DISPLAY_TIME_PATTERN = "HH:mm:ss"
    private const val DISPLAY_DATETIME_PATTERN = "MMM dd, yyyy HH:mm:ss"

    /**
     * Formats a Date to ISO 8601 string
     */
    fun formatToIso8601(date: Date): String {
        val sdf = SimpleDateFormat(ISO_8601_PATTERN, Locale.US)
        sdf.timeZone = TimeZone.getTimeZone("UTC")
        return sdf.format(date)
    }

    /**
     * Parses an ISO 8601 string to Date
     */
    fun parseFromIso8601(dateString: String): Date? {
        return try {
            val sdf = SimpleDateFormat(ISO_8601_PATTERN, Locale.US)
            sdf.timeZone = TimeZone.getTimeZone("UTC")
            sdf.parse(dateString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Formats a Date to display format (e.g., "Jan 15, 2024")
     */
    fun formatToDisplayDate(date: Date): String {
        val sdf = SimpleDateFormat(DISPLAY_DATE_PATTERN, Locale.US)
        return sdf.format(date)
    }

    /**
     * Formats a Date to display time format (e.g., "14:30:45")
     */
    fun formatToDisplayTime(date: Date): String {
        val sdf = SimpleDateFormat(DISPLAY_TIME_PATTERN, Locale.US)
        return sdf.format(date)
    }

    /**
     * Formats a Date to display date and time format (e.g., "Jan 15, 2024 14:30:45")
     */
    fun formatToDisplayDateTime(date: Date): String {
        val sdf = SimpleDateFormat(DISPLAY_DATETIME_PATTERN, Locale.US)
        return sdf.format(date)
    }

    /**
     * Gets the current time in milliseconds
     */
    fun getCurrentTimeMillis(): Long = System.currentTimeMillis()

    /**
     * Checks if a timestamp is older than specified duration
     */
    fun isOlderThan(timestamp: Long, durationMillis: Long): Boolean {
        return (System.currentTimeMillis() - timestamp) > durationMillis
    }

    /**
     * Checks if a timestamp is within the specified duration
     */
    fun isWithin(timestamp: Long, durationMillis: Long): Boolean {
        return !isOlderThan(timestamp, durationMillis)
    }

    /**
     * Gets a relative time string (e.g., "2 hours ago", "Yesterday")
     */
    fun getRelativeTimeString(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 7 -> formatToDisplayDate(Date(timestamp))
            days > 1 -> "$days days ago"
            days == 1L -> "Yesterday"
            hours > 1 -> "$hours hours ago"
            hours == 1L -> "1 hour ago"
            minutes > 1 -> "$minutes minutes ago"
            minutes == 1L -> "1 minute ago"
            seconds > 10 -> "$seconds seconds ago"
            else -> "Just now"
        }
    }
}
