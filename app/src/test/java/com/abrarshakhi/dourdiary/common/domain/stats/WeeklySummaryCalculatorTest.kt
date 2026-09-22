package com.abrarshakhi.dourdiary.common.domain.stats

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.Run
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class WeeklySummaryCalculatorTest {

    private val zone = ZoneId.of("Europe/Berlin")
    private val wednesday = LocalDate.of(2026, 9, 16)

    private fun runAt(
        dateTime: LocalDateTime,
        distanceMeters: Double = 5_000.0,
        movingMillis: Long = 1_500_000L,
        isComplete: Boolean = true,
    ) = Run(
        id = dateTime.hashCode().toLong(),
        startedAtEpochMillis = dateTime.atZone(zone).toInstant().toEpochMilli(),
        finishedAtEpochMillis = dateTime.atZone(zone).toInstant().toEpochMilli() + movingMillis,
        distance = Distance(distanceMeters),
        movingDurationMillis = movingMillis,
        elapsedDurationMillis = movingMillis,
        accumulatedPausedMillis = 0L,
        isComplete = isComplete,
    )

    @Test
    fun `the week always has seven days even with no runs`() {
        val summary = WeeklySummaryCalculator.summarise(
            runs = emptyList(),
            today = wednesday,
            firstDayOfWeek = DayOfWeek.MONDAY,
            zone = zone,
        )

        assertEquals(7, summary.days.size)
        assertEquals(0, summary.runCount)
        assertEquals(0.0, summary.totalDistance.meters, 1e-9)
        assertNull(summary.busiestDayDistance)
    }

    @Test
    fun `the week starts on the configured day`() {
        assertEquals(
            LocalDate.of(2026, 9, 14),
            WeeklySummaryCalculator.startOfWeek(wednesday, DayOfWeek.MONDAY),
        )
        assertEquals(
            LocalDate.of(2026, 9, 13),
            WeeklySummaryCalculator.startOfWeek(wednesday, DayOfWeek.SUNDAY),
        )
        assertEquals(
            LocalDate.of(2026, 9, 12),
            WeeklySummaryCalculator.startOfWeek(wednesday, DayOfWeek.SATURDAY),
        )
    }

    @Test
    fun `a run lands on the day it happened locally`() {
        val summary = WeeklySummaryCalculator.summarise(
            runs = listOf(runAt(LocalDateTime.of(2026, 9, 15, 0, 30))),
            today = wednesday,
            firstDayOfWeek = DayOfWeek.MONDAY,
            zone = zone,
        )

        val tuesday = summary.days.single { it.date == LocalDate.of(2026, 9, 15) }
        assertEquals(1, tuesday.runCount)
        assertEquals(5_000.0, tuesday.distance.meters, 1e-9)
    }

    @Test
    fun `a late evening run does not slip into the next day`() {
        val summary = WeeklySummaryCalculator.summarise(
            runs = listOf(runAt(LocalDateTime.of(2026, 9, 15, 23, 45))),
            today = wednesday,
            firstDayOfWeek = DayOfWeek.MONDAY,
            zone = zone,
        )

        assertEquals(1, summary.days.single { it.date == LocalDate.of(2026, 9, 15) }.runCount)
        assertEquals(0, summary.days.single { it.date == LocalDate.of(2026, 9, 16) }.runCount)
    }

    @Test
    fun `several runs on one day are added together`() {
        val summary = WeeklySummaryCalculator.summarise(
            runs = listOf(
                runAt(LocalDateTime.of(2026, 9, 16, 7, 0), distanceMeters = 5_000.0),
                runAt(LocalDateTime.of(2026, 9, 16, 18, 0), distanceMeters = 3_000.0),
            ),
            today = wednesday,
            firstDayOfWeek = DayOfWeek.MONDAY,
            zone = zone,
        )

        val today = summary.days.single { it.date == wednesday }
        assertEquals(2, today.runCount)
        assertEquals(8_000.0, today.distance.meters, 1e-9)
        assertEquals(8_000.0, summary.totalDistance.meters, 1e-9)
    }

    @Test
    fun `runs from last week are excluded from the totals`() {
        val summary = WeeklySummaryCalculator.summarise(
            runs = listOf(
                runAt(LocalDateTime.of(2026, 9, 16, 7, 0), distanceMeters = 5_000.0),
                runAt(LocalDateTime.of(2026, 9, 10, 7, 0), distanceMeters = 9_000.0),
            ),
            today = wednesday,
            firstDayOfWeek = DayOfWeek.MONDAY,
            zone = zone,
        )

        assertEquals(1, summary.runCount)
        assertEquals(5_000.0, summary.totalDistance.meters, 1e-9)
    }

    @Test
    fun `a run still in progress is not counted`() {
        val summary = WeeklySummaryCalculator.summarise(
            runs = listOf(runAt(LocalDateTime.of(2026, 9, 16, 7, 0), isComplete = false)),
            today = wednesday,
            firstDayOfWeek = DayOfWeek.MONDAY,
            zone = zone,
        )

        assertEquals(0, summary.runCount)
    }

    @Test
    fun `the busiest day scales the chart`() {
        val summary = WeeklySummaryCalculator.summarise(
            runs = listOf(
                runAt(LocalDateTime.of(2026, 9, 14, 7, 0), distanceMeters = 4_000.0),
                runAt(LocalDateTime.of(2026, 9, 16, 7, 0), distanceMeters = 12_000.0),
            ),
            today = wednesday,
            firstDayOfWeek = DayOfWeek.MONDAY,
            zone = zone,
        )

        assertEquals(12_000.0, summary.busiestDayDistance!!.meters, 1e-9)
    }

    @Test
    fun `average pace is derived from the week's totals`() {
        val summary = WeeklySummaryCalculator.summarise(
            runs = listOf(
                runAt(LocalDateTime.of(2026, 9, 16, 7, 0), 10_000.0, movingMillis = 3_000_000L),
            ),
            today = wednesday,
            firstDayOfWeek = DayOfWeek.MONDAY,
            zone = zone,
        )

        assertEquals(300.0, summary.averagePace!!.secondsPerKilometer, 1e-6)
    }
}
