package com.abrarshakhi.dourdiary.features.settings.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.abrarshakhi.dourdiary.R
import android.content.Intent
import androidx.core.net.toUri
import androidx.compose.ui.platform.LocalContext
import com.abrarshakhi.dourdiary.BuildConfig
import com.abrarshakhi.dourdiary.common.domain.model.AppTheme
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem

@Composable
fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    onOpenLicenses: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val privacyPolicyUrl = stringResource(R.string.privacy_policy_url)
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
    ) {
        SectionHeader(text = stringResource(R.string.settings_appearance))

        Column(Modifier.selectableGroup()) {
            AppTheme.entries.forEach { theme ->
                val selected = state.theme == theme
                ListItem(
                    headlineContent = { Text(stringResource(theme.labelRes())) },
                    leadingContent = {
                        RadioButton(selected = selected, onClick = null)
                    },
                    modifier = Modifier.selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onEvent(SettingsEvent.ThemeSelected(theme)) },
                    ),
                )
            }
        }

        HorizontalDivider()
        SectionHeader(text = stringResource(R.string.settings_units))

        Column(Modifier.selectableGroup()) {
            UnitSystem.entries.forEach { unitSystem ->
                val selected = state.unitSystem == unitSystem
                ListItem(
                    headlineContent = { Text(stringResource(unitSystem.labelRes())) },
                    leadingContent = { RadioButton(selected = selected, onClick = null) },
                    modifier = Modifier.selectable(
                        selected = selected,
                        role = Role.RadioButton,
                        onClick = { onEvent(SettingsEvent.UnitSystemSelected(unitSystem)) },
                    ),
                )
            }
        }

        HorizontalDivider()
        SectionHeader(text = stringResource(R.string.settings_cues))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_audio_cues)) },
            supportingContent = { Text(stringResource(R.string.settings_audio_cues_summary)) },
            trailingContent = {
                Switch(
                    checked = state.audioCuesEnabled,
                    onCheckedChange = { onEvent(SettingsEvent.AudioCuesToggled(it)) },
                )
            },
        )

        Column(Modifier.selectableGroup()) {
            state.cueIntervalOptions.forEach { interval ->
                val selected = state.cueIntervalUnits == interval
                ListItem(
                    headlineContent = {
                        Text(
                            stringResource(
                                R.string.settings_cue_every,
                                formatInterval(interval),
                                stringResource(state.unitSystem.unitLabelRes()),
                            ),
                        )
                    },
                    leadingContent = { RadioButton(selected = selected, onClick = null) },
                    modifier = Modifier.selectable(
                        selected = selected,
                        enabled = state.audioCuesEnabled,
                        role = Role.RadioButton,
                        onClick = { onEvent(SettingsEvent.CueIntervalSelected(interval)) },
                    ),
                )
            }
        }

        HorizontalDivider()
        SectionHeader(text = stringResource(R.string.settings_battery))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_battery_title)) },
            supportingContent = {
                Text(
                    stringResource(
                        if (state.batteryOptimisationExempt) {
                            R.string.settings_battery_exempt
                        } else {
                            R.string.settings_battery_restricted
                        },
                    ),
                )
            },
            modifier = Modifier.clickable(role = Role.Button) {
                onEvent(SettingsEvent.BatteryOptimisationClicked)
            },
        )
        HorizontalDivider()
        SectionHeader(text = stringResource(R.string.settings_about))

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_version)) },
            supportingContent = { Text(BuildConfig.VERSION_NAME) },
        )

        ListItem(
            headlineContent = { Text(stringResource(R.string.settings_licenses)) },
            supportingContent = { Text(stringResource(R.string.settings_licenses_summary)) },
            modifier = Modifier.clickable(onClick = onOpenLicenses),
        )

        if (privacyPolicyUrl.isNotBlank()) {
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_privacy)) },
                supportingContent = { Text(stringResource(R.string.settings_privacy_summary)) },
                modifier = Modifier.clickable {
                    runCatching {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, privacyPolicyUrl.toUri()),
                        )
                    }
                },
            )
        }
    }
}

@Composable
private fun SectionHeader(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

private fun AppTheme.labelRes(): Int = when (this) {
    AppTheme.SYSTEM -> R.string.settings_theme_system
    AppTheme.LIGHT -> R.string.settings_theme_light
    AppTheme.DARK -> R.string.settings_theme_dark
}

private fun UnitSystem.labelRes(): Int = when (this) {
    UnitSystem.METRIC -> R.string.settings_units_metric
    UnitSystem.IMPERIAL -> R.string.settings_units_imperial
}

private fun UnitSystem.unitLabelRes(): Int = when (this) {
    UnitSystem.METRIC -> R.string.unit_kilometers_short
    UnitSystem.IMPERIAL -> R.string.unit_miles_short
}

private fun formatInterval(units: Double): String =
    if (units == units.toLong().toDouble()) units.toLong().toString() else units.toString()
