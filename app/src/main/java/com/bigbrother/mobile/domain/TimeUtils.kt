package com.bigbrother.mobile.domain

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.max

object TimeUtils {
    val zoneId: ZoneId = ZoneId.systemDefault()
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
    private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val timeFormatter24 = DateTimeFormatter.ofPattern("HH:mm")
    private val timeFormatter12 = DateTimeFormatter.ofPattern("hh:mm a")

    fun now(): Long = System.currentTimeMillis()

    fun toLocalDateTime(millis: Long): LocalDateTime = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDateTime()
    fun toLocalDate(millis: Long): LocalDate = Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
    fun startOfDay(date: LocalDate): Long = date.atStartOfDay(zoneId).toInstant().toEpochMilli()
    fun nextDayStart(millis: Long): Long = toLocalDate(millis).plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
    fun formatDateTime(millis: Long): String = dateTimeFormatter.format(toLocalDateTime(millis))
    fun formatDate(millis: Long): String = dateFormatter.format(toLocalDateTime(millis))
    fun formatTime(millis: Long, use24Hour: Boolean): String = if (use24Hour) timeFormatter24.format(toLocalDateTime(millis)) else timeFormatter12.format(toLocalDateTime(millis))
    fun formatClock(millis: Long, showDate: Boolean, use24Hour: Boolean): String = buildString {
        if (showDate) append(formatDate(millis)).append(' ')
        append(formatTime(millis, use24Hour))
    }

    fun parseDateTime(text: String): Long? = try {
        LocalDateTime.parse(text, dateTimeFormatter).atZone(zoneId).toInstant().toEpochMilli()
    } catch (_: Exception) { null }

    fun parseDate(text: String): LocalDate? = try {
        LocalDate.parse(text, dateFormatter)
    } catch (_: Exception) { null }

    fun parseTime(text: String): LocalTime? = try {
        LocalTime.parse(text, DateTimeFormatter.ofPattern("HH:mm"))
    } catch (_: Exception) { null }

    fun clampRange(start: Long, end: Long): Pair<Long, Long> = if (end >= start) start to end else end to start

    fun dayRange(date: LocalDate): LongRange {
        val start = startOfDay(date)
        val end = date.plusDays(1).atStartOfDay(zoneId).toInstant().toEpochMilli()
        return start until end
    }

    fun weekStart(date: LocalDate, firstDay: DayOfWeek): LocalDate {
        var d = date
        while (d.dayOfWeek != firstDay) d = d.minusDays(1)
        return d
    }

    fun currentSemesterWeek(date: LocalDate, semesterStart: LocalDate, firstDay: DayOfWeek): Int {
        val start = weekStart(semesterStart, firstDay)
        val current = weekStart(date, firstDay)
        val days = max(0, current.toEpochDay() - start.toEpochDay()).toInt()
        return days / 7 + 1
    }
}
