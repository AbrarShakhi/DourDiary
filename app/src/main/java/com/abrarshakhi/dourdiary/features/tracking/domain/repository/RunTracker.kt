package com.abrarshakhi.dourdiary.features.tracking.domain.repository

import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunSnapshot
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

interface RunTracker {

    val snapshot: StateFlow<RunSnapshot>

    val activeRunId: StateFlow<Long?>

    val route: StateFlow<List<GeoPoint>>

    val recoverableRun: StateFlow<Run?>

    val finishedRuns: SharedFlow<Long>

    fun start()

    fun pause()

    fun resume()

    fun stop()

    suspend fun checkForRecoverableRun()

    fun resumeRecoveredRun()

    suspend fun discardRecoveredRun()
}
