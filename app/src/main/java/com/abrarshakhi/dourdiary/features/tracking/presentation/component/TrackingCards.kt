package com.abrarshakhi.dourdiary.features.tracking.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.format.RunFormatter
import com.abrarshakhi.dourdiary.features.tracking.presentation.LocationPermissionState

@Composable
fun LocationPermissionCard(
    permission: LocationPermissionState,
    onGrantClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.permission_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    if (permission == LocationPermissionState.PERMANENTLY_DENIED) {
                        R.string.permission_body_settings
                    } else {
                        R.string.permission_body
                    },
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onGrantClicked) {
                    Text(
                        stringResource(
                            if (permission == LocationPermissionState.PERMANENTLY_DENIED) {
                                R.string.permission_open_settings
                            } else {
                                R.string.permission_grant
                            },
                        ),
                    )
                }
            }
        }
    }
}

@Composable
fun BatteryAdviceCard(
    onActionClicked: () -> Unit,
    onDismissClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.battery_advice_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(R.string.battery_advice_body),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDismissClicked) {
                    Text(stringResource(R.string.battery_advice_dismiss))
                }
                TextButton(onClick = onActionClicked) {
                    Text(stringResource(R.string.battery_advice_action))
                }
            }
        }
    }
}

@Composable
fun RecoveredRunCard(
    run: Run,
    unitSystem: UnitSystem,
    onResume: () -> Unit,
    onDiscard: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val unitLabel = stringResource(
        when (unitSystem) {
            UnitSystem.METRIC -> R.string.unit_kilometers_short
            UnitSystem.IMPERIAL -> R.string.unit_miles_short
        },
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        ),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                text = stringResource(R.string.recovery_title),
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = stringResource(
                    R.string.recovery_body,
                    RunFormatter.distance(run.distance, unitSystem),
                    unitLabel,
                    RunFormatter.duration(run.movingDurationMillis),
                ),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(top = 8.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = onDiscard) {
                    Text(stringResource(R.string.recovery_discard))
                }
                TextButton(onClick = onResume) {
                    Text(stringResource(R.string.recovery_resume))
                }
            }
        }
    }
}
