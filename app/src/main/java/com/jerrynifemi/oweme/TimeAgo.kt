package com.jerrynifemi.oweme

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** "2h ago" rather than a timestamp, because that is what people actually read. */
object TimeAgo {

    private val dayFormat = SimpleDateFormat("d MMM", Locale.getDefault())

    fun format(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = diff / 60_000L
        return when {
            minutes < 1L -> "just now"
            minutes < 60L -> "${minutes}m ago"
            minutes < 60L * 24L -> "${minutes / 60L}h ago"
            minutes < 60L * 24L * 7L -> "${minutes / (60L * 24L)}d ago"
            else -> dayFormat.format(Date(timestamp))
        }
    }
}
