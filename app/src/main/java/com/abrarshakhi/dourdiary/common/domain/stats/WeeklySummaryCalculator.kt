package com.abrarshakhi.dourdiary.common.domain.stats

import com.abrarshakhi.dourdiary.common.domain.model.DailyDistance
import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.WeeklySummary
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

object WeeklySummaryCalculator {

    fun summarise(
        runs: List<Run>,
        today: LocalDate,
        firstDayOfWeek: DayOfWeek,
        zone: ZoneId,
    ): WeeklySummary {
        val weekStart = startOfWeek(today, firstDayOfWeek)
        val week = (0..6).map { weekStart.plusDays(it.toLong()) }

        val runsByDay = runs
            .filter { it.isComplete }
            .groupBy { run ->
                Instant.ofEpochMilli(run.startedAtEpochMillis).atZone(zone).toLocalDate()
            }

        val days = week.map { date ->
            val dayRuns = runsByDay[date].orEmpty()
            DailyDistance(
                date = date,
                distance = Distance(dayRuns.sumOf { it.distance.meters }),
                runCount = dayRuns.size,
            )
        }

        val runsThisWeek = week.flatMap { runsByDay[it].orEmpty() }

        return WeeklySummary(
            days = days,
            totalDistance = Distance(runsThisWeek.sumOf { it.distance.meters }),
            totalMovingDurationMillis = runsThisWeek.sumOf { it.movingDurationMillis },
            runCount = runsThisWeek.size,
        )
    }

    fun startOfWeek(today: LocalDate, firstDayOfWeek: DayOfWeek): LocalDate {
        val daysSinceStart = (today.dayOfWeek.value - firstDayOfWeek.value + 7) % 7
        return today.minusDays(daysSinceStart.toLong())
    }
}
