package com.abrarshakhi.dourdiary.features.settings.presentation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import com.abrarshakhi.dourdiary.common.domain.model.AppTheme
import com.abrarshakhi.dourdiary.common.ui.theme.isDynamicColorSupported

@Composable
fun SettingsScreen(
    state: SettingsState,
    onEvent: (SettingsEvent) -> Unit,
    modifier: Modifier = Modifier,
) {
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

        if (isDynamicColorSupported) {
            HorizontalDivider()
            ListItem(
                headlineContent = { Text(stringResource(R.string.settings_dynamic_color)) },
                supportingContent = {
                    Text(stringResource(R.string.settings_dynamic_color_summary))
                },
                trailingContent = {
                    Switch(
                        checked = state.dynamicColor,
                        onCheckedChange = { onEvent(SettingsEvent.DynamicColorToggled(it)) },
                    )
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
