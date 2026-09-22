package com.abrarshakhi.dourdiary.features.onboarding.presentation

import app.cash.turbine.test
import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.testing.MainDispatcherRule
import com.abrarshakhi.dourdiary.features.tracking.data.session.FakeAppPreferencesRepository
import com.abrarshakhi.dourdiary.features.tracking.domain.LocationPermissionChecker
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakePermissionChecker(var granted: Boolean = false) : LocationPermissionChecker {
    override fun hasLocationPermission(): Boolean = granted
}

class OnboardingViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeAppPreferencesRepository()

    private fun viewModel(checker: FakePermissionChecker = FakePermissionChecker()) =
        OnboardingViewModel(
            appPreferencesRepository = preferences,
            permissionChecker = checker,
        )

    @Test
    fun `a permission already granted is recognised without asking again`() {
        val model = viewModel(FakePermissionChecker(granted = true))

        assertTrue(model.state.value.locationGranted)
    }

    @Test
    fun `granting is picked up when returning from the system dialog`() {
        val checker = FakePermissionChecker(granted = false)
        val model = viewModel(checker)
        assertFalse(model.state.value.locationGranted)

        checker.granted = true
        model.onEvent(OnboardingEvent.ScreenResumed)

        assertTrue(model.state.value.locationGranted)
    }

    @Test
    fun `asking for location raises the request rather than assuming`() = runTest {
        val model = viewModel()

        model.effect.test {
            model.onEvent(OnboardingEvent.GrantLocationClicked)
            assertEquals(OnboardingEffect.RequestLocationPermission, awaitItem())
        }
    }

    @Test
    fun `the chosen unit is saved as it is picked, not at the end`() = runTest {
        val model = viewModel()

        model.onEvent(OnboardingEvent.UnitSystemSelected(UnitSystem.IMPERIAL))

        assertEquals(UnitSystem.IMPERIAL, preferences.preferences.first().unitSystem)
    }

    @Test
    fun `finishing marks the walkthrough done so it does not return`() = runTest {
        val model = viewModel()

        model.effect.test {
            model.onEvent(OnboardingEvent.FinishClicked)
            assertEquals(OnboardingEffect.Finished, awaitItem())
        }
        assertTrue(preferences.preferences.first().hasCompletedOnboarding)
    }

    @Test
    fun `skipping also counts as done`() = runTest {
        val model = viewModel()

        model.effect.test {
            model.onEvent(OnboardingEvent.SkipClicked)
            assertEquals(OnboardingEffect.Finished, awaitItem())
        }
        assertTrue(preferences.preferences.first().hasCompletedOnboarding)
    }

    @Test
    fun `a unit chosen before skipping is still kept`() = runTest {
        val model = viewModel()

        model.onEvent(OnboardingEvent.UnitSystemSelected(UnitSystem.IMPERIAL))
        model.onEvent(OnboardingEvent.SkipClicked)

        val saved = preferences.preferences.first()
        assertEquals(UnitSystem.IMPERIAL, saved.unitSystem)
        assertTrue(saved.hasCompletedOnboarding)
    }

    @Test
    fun `a fresh install has not completed the walkthrough`() = runTest {
        assertFalse(AppPreferences().hasCompletedOnboarding)
    }
}
