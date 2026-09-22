package com.abrarshakhi.dourdiary.features.home.presentation

import app.cash.turbine.test
import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.testing.MainDispatcherRule
import com.abrarshakhi.dourdiary.features.tracking.data.session.FakeAppPreferencesRepository
import com.abrarshakhi.dourdiary.features.tracking.data.session.FakeRunRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.time.Clock
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Locale

class HomeViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val zone = ZoneId.of("Europe/Berlin")

    private val wednesday = LocalDateTime.of(2026, 9, 16, 10, 0)
    private val clock = Clock.fixed(wednesday.atZone(zone).toInstant(), zone)

    private val repository = FakeRunRepository()

    private fun viewModel(locale: Locale = Locale.UK) = HomeViewModel(
        runRepository = repository,
        appPreferencesRepository = FakeAppPreferencesRepository(
            AppPreferences(unitSystem = UnitSystem.METRIC),
        ),
        clock = clock,
        locale = locale,
    )

    private fun addRun(
        at: LocalDateTime,
        distanceMeters: Double = 5_000.0,
        movingMillis: Long = 1_500_000L,
    ): Run {
        val id = repository.runs.size + 1L
        val run = Run(
            id = id,
            startedAtEpochMillis = at.atZone(zone).toInstant().toEpochMilli(),
            finishedAtEpochMillis = at.atZone(zone).toInstant().toEpochMilli() + movingMillis,
            distance = Distance(distanceMeters),
            movingDurationMillis = movingMillis,
            elapsedDurationMillis = movingMillis,
            accumulatedPausedMillis = 0L,
            isComplete = true,
        )
        repository.put(run)
        return run
    }

    @Test
    fun `an empty week still shows seven days`() {
        val state = viewModel().state.value

        assertEquals(7, state.week.days.size)
        assertEquals(0, state.week.runCount)
        assertTrue(state.recentRuns.isEmpty())
    }

    @Test
    fun `today is exposed so the chart can mark it`() {
        assertEquals(LocalDate.of(2026, 9, 16), viewModel().state.value.today)
    }

    @Test
    fun `the week totals only this week's runs`() {
        addRun(LocalDateTime.of(2026, 9, 14, 7, 0), distanceMeters = 6_000.0)
        addRun(LocalDateTime.of(2026, 9, 16, 7, 0), distanceMeters = 4_000.0)
        addRun(LocalDateTime.of(2026, 9, 9, 7, 0), distanceMeters = 20_000.0)

        val state = viewModel().state.value

        assertEquals(2, state.week.runCount)
        assertEquals(10_000.0, state.week.totalDistance.meters, 1e-9)
    }

    @Test
    fun `the locale decides which day the week starts on`() {
        addRun(LocalDateTime.of(2026, 9, 13, 7, 0), distanceMeters = 7_000.0)

        assertEquals(0, viewModel(Locale.UK).state.value.week.runCount)
        assertEquals(1, viewModel(Locale.US).state.value.week.runCount)
        assertEquals(
            DayOfWeek.MONDAY,
            viewModel(Locale.UK).state.value.week.days.first().date.dayOfWeek,
        )
        assertEquals(
            DayOfWeek.SUNDAY,
            viewModel(Locale.US).state.value.week.days.first().date.dayOfWeek,
        )
    }

    @Test
    fun `the feed shows recent runs`() {
        addRun(LocalDateTime.of(2026, 9, 16, 7, 0))
        addRun(LocalDateTime.of(2026, 9, 15, 7, 0))

        assertEquals(2, viewModel().state.value.recentRuns.size)
    }

    @Test
    fun `tapping a run opens it`() = runTest {
        val model = viewModel()

        model.effect.test {
            model.onEvent(HomeEvent.RunClicked(7L))
            assertEquals(HomeEffect.OpenRun(7L), awaitItem())
        }
    }

    @Test
    fun `the start button sends you to the record screen`() = runTest {
        val model = viewModel()

        model.effect.test {
            model.onEvent(HomeEvent.StartRunClicked)
            assertEquals(HomeEffect.OpenRecord, awaitItem())
        }
    }

    @Test
    fun `resuming recomputes the week instead of showing a stale one`() {
        val model = viewModel()
        assertEquals(0, model.state.value.week.runCount)

        addRun(LocalDateTime.of(2026, 9, 16, 7, 0))
        model.onEvent(HomeEvent.ScreenResumed)

        assertEquals(1, model.state.value.week.runCount)
    }

    @Test
    fun `a run recorded just after midnight counts for that day, not the day before`() {
        addRun(LocalDateTime.of(2026, 9, 16, 0, 15))

        val today = viewModel().state.value.week.days.single {
            it.date == LocalDate.of(2026, 9, 16)
        }

        assertEquals(1, today.runCount)
    }

    @Test
    fun `the clock is injected, not read from the system`() {
        assertEquals(
            LocalDate.ofInstant(Instant.now(clock), zone),
            viewModel().state.value.today,
        )
    }
}
