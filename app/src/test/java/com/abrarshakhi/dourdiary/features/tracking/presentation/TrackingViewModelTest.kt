package com.abrarshakhi.dourdiary.features.tracking.presentation

import app.cash.turbine.test
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.testing.MainDispatcherRule
import com.abrarshakhi.dourdiary.features.tracking.data.session.FakeAppPreferencesRepository
import com.abrarshakhi.dourdiary.features.tracking.domain.LocationPermissionChecker
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunSnapshot
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunStatus
import com.abrarshakhi.dourdiary.features.tracking.domain.repository.RunTracker
import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.power.BackgroundRestrictionChecker
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeRunTracker : RunTracker {
    override val snapshot = MutableStateFlow(RunSnapshot())
    override val activeRunId = MutableStateFlow<Long?>(null)
    override val route = MutableStateFlow<List<GeoPoint>>(emptyList())
    override val recoverableRun = MutableStateFlow<Run?>(null)
    override val finishedRuns = MutableSharedFlow<Long>(extraBufferCapacity = 4)

    var startCount = 0
    var pauseCount = 0
    var resumeCount = 0
    var stopCount = 0
    var resumeRecoveredCount = 0
    var discardRecoveredCount = 0

    override fun start() { startCount++ }
    override fun pause() { pauseCount++ }
    override fun resume() { resumeCount++ }
    override fun stop() { stopCount++ }
    override suspend fun checkForRecoverableRun() = Unit
    override fun resumeRecoveredRun() { resumeRecoveredCount++ }
    override suspend fun discardRecoveredRun() { discardRecoveredCount++ }
}

private class FakePermissionChecker(var granted: Boolean) : LocationPermissionChecker {
    override fun hasLocationPermission(): Boolean = granted
}

private class FakeRestrictionChecker(var exempt: Boolean) : BackgroundRestrictionChecker {
    override fun isExemptFromBatteryOptimisation(): Boolean = exempt
}

class TrackingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val tracker = FakeRunTracker()

    private fun viewModel(
        permissionGranted: Boolean = true,
        preferences: AppPreferences = AppPreferences(),
        checker: FakePermissionChecker = FakePermissionChecker(permissionGranted),
        restrictionChecker: FakeRestrictionChecker = FakeRestrictionChecker(exempt = true),
        preferencesRepository: FakeAppPreferencesRepository =
            FakeAppPreferencesRepository(preferences),
    ) = TrackingViewModel(
        runTracker = tracker,
        permissionChecker = checker,
        restrictionChecker = restrictionChecker,
        appPreferencesRepository = preferencesRepository,
    )

    @Test
    fun `permission is read at construction rather than assumed`() {
        assertEquals(LocationPermissionState.GRANTED, viewModel(permissionGranted = true).state.value.permission)
        assertEquals(LocationPermissionState.UNKNOWN, viewModel(permissionGranted = false).state.value.permission)
    }

    @Test
    fun `starting without permission asks for it instead of starting a run`() = runTest {
        val model = viewModel(permissionGranted = false)

        model.effect.test {
            model.onEvent(TrackingEvent.StartClicked)
            assertEquals(TrackingEffect.RequestLocationPermission, awaitItem())
        }
        assertEquals("A run started without permission", 0, tracker.startCount)
    }

    @Test
    fun `starting with permission starts the run`() {
        val model = viewModel(permissionGranted = true)

        model.onEvent(TrackingEvent.StartClicked)

        assertEquals(1, tracker.startCount)
    }

    @Test
    fun `a granted result unlocks the start button`() {
        val model = viewModel(permissionGranted = false)

        model.onEvent(TrackingEvent.PermissionResult(granted = true, canAskAgain = true))

        assertEquals(LocationPermissionState.GRANTED, model.state.value.permission)
        assertTrue(model.state.value.canStart)
    }

    @Test
    fun `a soft denial can be asked again`() {
        val model = viewModel(permissionGranted = false)

        model.onEvent(TrackingEvent.PermissionResult(granted = false, canAskAgain = true))

        assertEquals(LocationPermissionState.DENIED, model.state.value.permission)
        assertFalse(model.state.value.canStart)
    }

    @Test
    fun `a final denial is recognised as needing settings`() {
        val model = viewModel(permissionGranted = false)

        model.onEvent(TrackingEvent.PermissionResult(granted = false, canAskAgain = false))

        assertEquals(LocationPermissionState.PERMANENTLY_DENIED, model.state.value.permission)
    }

    @Test
    fun `after a final denial the button opens settings instead of the dialog`() = runTest {
        val model = viewModel(permissionGranted = false)
        model.onEvent(TrackingEvent.PermissionResult(granted = false, canAskAgain = false))

        model.effect.test {
            model.onEvent(TrackingEvent.GrantPermissionClicked)
            assertEquals(TrackingEffect.OpenAppSettings, awaitItem())
        }
    }

    @Test
    fun `a final denial survives a resume that learns nothing new`() {
        val checker = FakePermissionChecker(granted = false)
        val model = viewModel(checker = checker)
        model.onEvent(TrackingEvent.PermissionResult(granted = false, canAskAgain = false))

        model.onEvent(TrackingEvent.ScreenResumed)

        assertEquals(
            "The screen forgot that asking again is pointless",
            LocationPermissionState.PERMANENTLY_DENIED,
            model.state.value.permission,
        )
    }

    @Test
    fun `granting in system settings is picked up on resume`() {
        val checker = FakePermissionChecker(granted = false)
        val model = viewModel(checker = checker)
        model.onEvent(TrackingEvent.PermissionResult(granted = false, canAskAgain = false))

        checker.granted = true
        model.onEvent(TrackingEvent.ScreenResumed)

        assertEquals(LocationPermissionState.GRANTED, model.state.value.permission)
    }

    @Test
    fun `controls delegate to the tracker`() {
        val model = viewModel()

        model.onEvent(TrackingEvent.PauseClicked)
        model.onEvent(TrackingEvent.ResumeClicked)
        model.onEvent(TrackingEvent.StopClicked)
        model.onEvent(TrackingEvent.ResumeRecoveredRunClicked)

        assertEquals(1, tracker.pauseCount)
        assertEquals(1, tracker.resumeCount)
        assertEquals(1, tracker.stopCount)
        assertEquals(1, tracker.resumeRecoveredCount)
    }

    @Test
    fun `finishing a run opens its summary`() = runTest {
        val model = viewModel()

        model.effect.test {
            tracker.finishedRuns.emit(42L)
            assertEquals(TrackingEffect.OpenRunSummary(42L), awaitItem())
        }
    }

    @Test
    fun `battery advice is offered only when the system may kill the run`() {
        assertFalse(
            "Advice was pushed at a runner who is already exempt",
            viewModel(restrictionChecker = FakeRestrictionChecker(exempt = true))
                .state.value.showBatteryAdvice,
        )
        assertTrue(
            viewModel(restrictionChecker = FakeRestrictionChecker(exempt = false))
                .state.value.showBatteryAdvice,
        )
    }

    @Test
    fun `battery advice stays hidden once it has been dismissed`() {
        val model = viewModel(
            preferences = AppPreferences(batteryAdviceDismissed = true),
            restrictionChecker = FakeRestrictionChecker(exempt = false),
        )

        assertTrue(model.state.value.powerRestricted)
        assertFalse(model.state.value.showBatteryAdvice)
    }

    @Test
    fun `dismissing the advice persists so it does not come back next launch`() = runTest {
        val preferences = FakeAppPreferencesRepository()
        val model = viewModel(
            restrictionChecker = FakeRestrictionChecker(exempt = false),
            preferencesRepository = preferences,
        )

        model.onEvent(TrackingEvent.BatteryAdviceDismissed)

        assertFalse(model.state.value.showBatteryAdvice)
        preferences.preferences.test {
            assertTrue("The dismissal was not written down", awaitItem().batteryAdviceDismissed)
        }
    }

    @Test
    fun `the advice action opens the system battery screen`() = runTest {
        val model = viewModel(restrictionChecker = FakeRestrictionChecker(exempt = false))

        model.effect.test {
            model.onEvent(TrackingEvent.BatteryAdviceActionClicked)
            assertEquals(TrackingEffect.OpenBatteryOptimisationSettings, awaitItem())
        }
        assertTrue(
            "Tapping the action silently dismissed advice that may not have been acted on",
            model.state.value.showBatteryAdvice,
        )
    }

    @Test
    fun `granting the exemption in system settings is picked up on resume`() {
        val restrictionChecker = FakeRestrictionChecker(exempt = false)
        val model = viewModel(restrictionChecker = restrictionChecker)
        assertTrue(model.state.value.showBatteryAdvice)

        restrictionChecker.exempt = true
        model.onEvent(TrackingEvent.ScreenResumed)

        assertFalse(model.state.value.showBatteryAdvice)
    }

    @Test
    fun `battery advice keeps out of the way while a run is recording`() {
        val model = viewModel(restrictionChecker = FakeRestrictionChecker(exempt = false))

        tracker.snapshot.value = RunSnapshot(status = RunStatus.ACTIVE)

        assertFalse(model.state.value.showBatteryAdvice)
    }

    @Test
    fun `the screen reflects the run and the chosen units`() {
        val model = viewModel(preferences = AppPreferences(unitSystem = UnitSystem.IMPERIAL))

        tracker.route.value = listOf(GeoPoint(52.52, 13.405))
        tracker.snapshot.value = RunSnapshot(hasPoorSignal = true)

        assertEquals(UnitSystem.IMPERIAL, model.state.value.unitSystem)
        assertEquals(1, model.state.value.route.size)
        assertTrue(model.state.value.snapshot.hasPoorSignal)
    }
}
