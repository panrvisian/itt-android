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

data class GroupStat(
    val groupId: String,
    val groupName: String,
    val groupColorArgb: Int,
    val total: Duration,
    val items: List<EventStat>
)

data class StatsResult(
    val items: List<EventStat>,
    val groups: List<GroupStat>,
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
        val groupStatMap = linkedMapOf<String, MutableGroupBuilder>()
        val uniqueMinutes = mutableSetOf<Long>()
        val activeDays = mutableSetOf<LocalDate>()

        for (record in records) {
            val event = eventMap[record.eventId] ?: continue
            val end = record.endTime ?: TimeUtils.now()
            val clippedStart = maxOf(record.startTime, rangeStart)
            val clippedEnd = minOf(end, rangeEnd)
            if (clippedEnd <= clippedStart) continue

            val durationMillis = clippedEnd - clippedStart
            val recordDay = TimeUtils.toLocalDate(clippedStart)
            activeDays += recordDay

            val entry = statMap.getOrPut(record.eventId) {
                MutableBuilder(event.id, record.eventNameSnapshot, record.groupColorArgbSnapshot)
            }
            entry.count += 1
            entry.totalMillis += durationMillis
            entry.daySet += recordDay

            val groupEntry = groupStatMap.getOrPut(record.groupIdSnapshot) {
                MutableGroupBuilder(
                    groupId = record.groupIdSnapshot,
                    groupName = record.groupNameSnapshot,
                    groupColorArgb = record.groupColorArgbSnapshot
                )
            }
            groupEntry.totalMillis += durationMillis
            val groupEventEntry = groupEntry.eventStats.getOrPut(record.eventId) {
                MutableBuilder(event.id, record.eventNameSnapshot, record.groupColorArgbSnapshot)
            }
            groupEventEntry.count += 1
            groupEventEntry.totalMillis += durationMillis
            groupEventEntry.daySet += recordDay

            val minuteStart = clippedStart / 60000L
            val minuteEnd = (clippedEnd + 59999L) / 60000L
            for (m in minuteStart until minuteEnd) uniqueMinutes += m
        }

        val items = statMap.values
            .map { it.toEventStat() }
            .sortedWith(eventStatComparator)

        val groups = groupStatMap.values
            .map { builder ->
                GroupStat(
                    groupId = builder.groupId,
                    groupName = builder.groupName,
                    groupColorArgb = builder.groupColorArgb,
                    total = Duration.ofMillis(builder.totalMillis),
                    items = builder.eventStats.values
                        .map { it.toEventStat() }
                        .sortedWith(eventStatComparator)
                )
            }
            .sortedWith(compareByDescending<GroupStat> { it.total }.thenBy { it.groupName })

        return StatsResult(
            items = items,
            groups = groups,
            uniqueTotal = Duration.ofMinutes(uniqueMinutes.size.toLong()),
            sumTotal = Duration.ofMillis(items.sumOf { it.total.toMillis() }),
            activeDays = activeDays.size
        )
    }

    fun rangeFor(kind: StatsRangeKind, anchorDate: LocalDate, semesterStart: LocalDate, weekStartDay: java.time.DayOfWeek, semesterWeeks: Int): Pair<Long, Long> {
        return when (kind) {
            StatsRangeKind.Today -> TimeUtils.startOfDay(anchorDate) to TimeUtils.startOfDay(anchorDate.plusDays(1))
            StatsRangeKind.Week -> {
                val startDate = TimeUtils.weekStart(anchorDate, weekStartDay)
                TimeUtils.startOfDay(startDate) to TimeUtils.startOfDay(startDate.plusDays(7))
            }
            StatsRangeKind.Month -> {
                val startDate = anchorDate.withDayOfMonth(1)
                TimeUtils.startOfDay(startDate) to TimeUtils.startOfDay(startDate.plusMonths(1))
            }
            StatsRangeKind.Semester -> {
                val startDate = semesterStart
                TimeUtils.startOfDay(startDate) to TimeUtils.startOfDay(startDate.plusWeeks(semesterWeeks.toLong()))
            }
        }
    }

    private val eventStatComparator =
        compareByDescending<EventStat> { it.total }.thenByDescending { it.count }.thenBy { it.eventName }

    private fun MutableBuilder.toEventStat() = EventStat(
        eventId = eventId,
        eventName = eventName,
        groupColorArgb = groupColorArgb,
        count = count,
        total = Duration.ofMillis(totalMillis),
        days = daySet.size
    )

    private data class MutableBuilder(
        val eventId: String,
        val eventName: String,
        val groupColorArgb: Int,
        var count: Int = 0,
        var totalMillis: Long = 0L,
        val daySet: MutableSet<LocalDate> = linkedSetOf()
    )

    private data class MutableGroupBuilder(
        val groupId: String,
        val groupName: String,
        val groupColorArgb: Int,
        var totalMillis: Long = 0L,
        val eventStats: MutableMap<String, MutableBuilder> = linkedMapOf()
    )
}