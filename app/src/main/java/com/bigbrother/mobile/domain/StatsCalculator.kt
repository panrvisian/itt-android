package com.bigbrother.mobile.domain

import com.bigbrother.mobile.data.EventEntity
import com.bigbrother.mobile.data.RecordEntity
import java.time.Duration
import java.time.LocalDate
import java.time.ZoneId

enum class StatsRangeKind { Today, Week, Month, Semester }

data class EventStat(
    val eventId: String,
    val eventName: String,
    val groupColorArgb: Int,
    val count: Int,
    val total: Duration,
    val days: Int
) {
    val average: Duration = if (count == 0) Duration.ZERO else Duration.ofMillis(total.toMillis() / count)
}

data class StatsResult(
    val items: List<EventStat>,
    val uniqueTotal: Duration,
    val sumTotal: Duration,
    val activeDays: Int
)

object StatsCalculator {
    fun compute(
        records: List<RecordEntity>,
        events: List<EventEntity>,
        rangeStart: Long,
        rangeEnd: Long,
        zoneId: ZoneId = TimeUtils.zoneId
    ): StatsResult {
        val eventMap = events.associateBy { it.id }
        val statMap = linkedMapOf<String, MutableBuilder>()
        val uniqueMinutes = mutableSetOf<Long>()
        val activeDays = mutableSetOf<LocalDate>()

        for (record in records) {
            val event = eventMap[record.eventId] ?: continue
            val end = record.endTime ?: TimeUtils.now()
            val clippedStart = maxOf(record.startTime, rangeStart)
            val clippedEnd = minOf(end, rangeEnd)
            if (clippedEnd <= clippedStart) continue
            activeDays += TimeUtils.toLocalDate(clippedStart)
            val entry = statMap.getOrPut(record.eventId) {
                MutableBuilder(event.id, record.eventNameSnapshot, record.groupColorArgbSnapshot)
            }
            entry.count += 1
            entry.totalMillis += clippedEnd - clippedStart
            val minuteStart = clippedStart / 60000L
            val minuteEnd = (clippedEnd + 59999L) / 60000L
            for (m in minuteStart until minuteEnd) uniqueMinutes += m
            entry.daySet += TimeUtils.toLocalDate(clippedStart)
        }

        val items = statMap.values
            .map { builder ->
                EventStat(
                    eventId = builder.eventId,
                    eventName = builder.eventName,
                    groupColorArgb = builder.groupColorArgb,
                    count = builder.count,
                    total = Duration.ofMillis(builder.totalMillis),
                    days = builder.daySet.size
                )
            }
            .sortedWith(compareByDescending<EventStat> { it.total }.thenByDescending { it.count }.thenBy { it.eventName })

        return StatsResult(
            items = items,
            uniqueTotal = Duration.ofMinutes(uniqueMinutes.size.toLong()),
            sumTotal = Duration.ofMillis(items.sumOf { it.total.toMillis() }),
            activeDays = activeDays.size
        )
    }

    fun rangeFor(kind: StatsRangeKind, today: LocalDate, semesterStart: LocalDate, weekStartDay: java.time.DayOfWeek, semesterWeeks: Int): Pair<Long, Long> {
        return when (kind) {
            StatsRangeKind.Today -> TimeUtils.startOfDay(today) to TimeUtils.startOfDay(today.plusDays(1))
            StatsRangeKind.Week -> {
                val startDate = TimeUtils.weekStart(today, weekStartDay)
                TimeUtils.startOfDay(startDate) to TimeUtils.startOfDay(startDate.plusDays(7))
            }
            StatsRangeKind.Month -> {
                val startDate = today.withDayOfMonth(1)
                TimeUtils.startOfDay(startDate) to TimeUtils.startOfDay(startDate.plusMonths(1))
            }
            StatsRangeKind.Semester -> {
                val startDate = semesterStart
                TimeUtils.startOfDay(startDate) to TimeUtils.startOfDay(startDate.plusWeeks(semesterWeeks.toLong()))
            }
        }
    }

    private data class MutableBuilder(
        val eventId: String,
        val eventName: String,
        val groupColorArgb: Int,
        var count: Int = 0,
        var totalMillis: Long = 0L,
        val daySet: MutableSet<LocalDate> = linkedSetOf()
    )
}
