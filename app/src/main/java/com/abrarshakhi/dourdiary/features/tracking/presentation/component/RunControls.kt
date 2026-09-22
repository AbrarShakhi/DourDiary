package com.abrarshakhi.dourdiary.features.tracking.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunStatus

@Composable
fun RunControls(
    status: RunStatus,
    enabled: Boolean,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    when (status) {
        RunStatus.IDLE, RunStatus.FINISHED -> Button(
            onClick = onStart,
            enabled = enabled,
            modifier = modifier
                .fillMaxWidth()
                .height(ControlHeight),
        ) {
            Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(28.dp))
            Text(
                text = stringResource(R.string.tracking_start),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(start = 8.dp),
            )
        }

        else -> Row(
            modifier = modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (status.isPaused) {
                Button(
                    onClick = onResume,
                    modifier = Modifier
                        .weight(1f)
                        .height(ControlHeight),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, Modifier.size(28.dp))
                    Text(stringResource(R.string.tracking_resume))
                }
            } else {
                Button(
                    onClick = onPause,
                    modifier = Modifier
                        .weight(1f)
                        .height(ControlHeight),
                ) {
                    Icon(Icons.Filled.Pause, contentDescription = null, Modifier.size(28.dp))
                    Text(stringResource(R.string.tracking_pause))
                }
            }

            Button(
                onClick = onStop,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer,
                ),
                modifier = Modifier
                    .weight(1f)
                    .height(ControlHeight),
            ) {
                Icon(Icons.Filled.Stop, contentDescription = null, Modifier.size(28.dp))
                Text(stringResource(R.string.tracking_finish))
            }
        }
    }
}

private val ControlHeight = 64.dp
