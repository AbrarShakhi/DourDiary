package com.abrarshakhi.dourdiary.features.settings.presentation

import app.cash.turbine.test
import com.abrarshakhi.dourdiary.common.domain.model.AppPreferences
import com.abrarshakhi.dourdiary.common.domain.model.AppTheme
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.domain.power.BackgroundRestrictionChecker
import com.abrarshakhi.dourdiary.common.testing.MainDispatcherRule
import com.abrarshakhi.dourdiary.features.tracking.data.session.FakeAppPreferencesRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

private class FakeRestrictionChecker(var exempt: Boolean) : BackgroundRestrictionChecker {
    override fun isExemptFromBatteryOptimisation(): Boolean = exempt
}

class SettingsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private fun viewModel(
        preferences: AppPreferences = AppPreferences(),
        repository: FakeAppPreferencesRepository = FakeAppPreferencesRepository(preferences),
        restrictionChecker: FakeRestrictionChecker = FakeRestrictionChecker(exempt = true),
    ) = SettingsViewModel(
        appPreferencesRepository = repository,
        restrictionChecker = restrictionChecker,
    )

    @Test
    fun `stored preferences are reflected on screen`() {
        val model = viewModel(
            AppPreferences(
                theme = AppTheme.DARK,
                unitSystem = UnitSystem.IMPERIAL,
                audioCuesEnabled = false,
                cueIntervalUnits = 5.0,
            ),
        )

        assertEquals(AppTheme.DARK, model.state.value.theme)
        assertEquals(UnitSystem.IMPERIAL, model.state.value.unitSystem)
        assertFalse(model.state.value.audioCuesEnabled)
        assertEquals(5.0, model.state.value.cueIntervalUnits, 0.0)
    }

    @Test
    fun `choosing a setting writes it through`() = runTest {
        val repository = FakeAppPreferencesRepository()
        val model = viewModel(repository = repository)

        model.onEvent(SettingsEvent.UnitSystemSelected(UnitSystem.IMPERIAL))
        model.onEvent(SettingsEvent.CueIntervalSelected(2.0))

        repository.preferences.test {
            val stored = awaitItem()
            assertEquals(UnitSystem.IMPERIAL, stored.unitSystem)
            assertEquals(2.0, stored.cueIntervalUnits, 0.0)
        }
    }

    @Test
    fun `the battery exemption is read at construction rather than assumed`() {
        assertTrue(
            viewModel(restrictionChecker = FakeRestrictionChecker(exempt = true))
                .state.value.batteryOptimisationExempt,
        )
        assertFalse(
            viewModel(restrictionChecker = FakeRestrictionChecker(exempt = false))
                .state.value.batteryOptimisationExempt,
        )
    }

    @Test
    fun `an exemption granted in system settings is picked up on resume`() {
        val restrictionChecker = FakeRestrictionChecker(exempt = false)
        val model = viewModel(restrictionChecker = restrictionChecker)

        restrictionChecker.exempt = true
        model.onEvent(SettingsEvent.ScreenResumed)

        assertTrue(model.state.value.batteryOptimisationExempt)
    }

    @Test
    fun `the battery row opens the system screen`() = runTest {
        val model = viewModel()

        model.effect.test {
            model.onEvent(SettingsEvent.BatteryOptimisationClicked)
            assertEquals(SettingsEffect.OpenBatteryOptimisationSettings, awaitItem())
        }
    }
}
