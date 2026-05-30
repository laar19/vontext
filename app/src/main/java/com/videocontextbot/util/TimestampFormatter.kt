package com.videocontextbot.util

object TimestampFormatter {
    fun format(seconds: Float): String {
        val totalSecs = seconds.toInt()
        val mins = totalSecs / 60
        val secs = totalSecs % 60
        return "${mins}m${secs}s"
    }

    fun formatLong(seconds: Float): String {
        val totalSecs = seconds.toInt()
        val hours = totalSecs / 3600
        val mins = (totalSecs % 3600) / 60
        val secs = totalSecs % 60
        return if (hours > 0) {
            "${hours}h ${mins}m ${secs}s"
        } else {
            "${mins}m ${secs}s"
        }
    }

    fun formatTimestamp(millis: Long): String {
        val seconds = millis / 1000
        val mins = seconds / 60
        val secs = seconds % 60
        return "${mins}m${secs}s"
    }
}
