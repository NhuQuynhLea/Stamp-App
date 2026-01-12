package com.lea.stamp.data

import kotlinx.datetime.*
import kotlin.time.Clock

object DateUtils {
    fun generateDayId(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val day = dateTime.dayOfMonth.toString().padStart(2, '0')
        val month = dateTime.monthNumber.toString().padStart(2, '0')
        val year = dateTime.year.toString()
        return "$day$month$year"
    }
    
    fun formatDateLabel(timestamp: Long): String {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val today = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        val daysDiff = (today.date.toEpochDays() - dateTime.date.toEpochDays())
        
        return when (daysDiff) {
            0L -> "Today"
            1L -> "Yesterday"
            else -> {
                val month = dateTime.month.name.lowercase().replaceFirstChar { it.uppercase() }.substring(0, 3)
                "${dateTime.dayOfMonth} $month"
            }
        }
    }
    
    fun getStartOfDay(timestamp: Long): Long {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val startOfDay = LocalDateTime(dateTime.date, LocalTime(0, 0, 0))
        return startOfDay.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
    
    fun getEndOfDay(timestamp: Long): Long {
        val instant = Instant.fromEpochMilliseconds(timestamp)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        val endOfDay = LocalDateTime(dateTime.date, LocalTime(23, 59, 59, 999_999_999))
        return endOfDay.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
    
    fun getTodayStartTimestamp(): Long {
        val today = kotlin.time.Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val startOfDay = LocalDateTime(today.date, LocalTime(0, 0, 0))
        return startOfDay.toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
    }
}
