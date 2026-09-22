package com.abrarshakhi.dourdiary.features.tracking.data

import android.content.Context
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.domain.model.Run
import com.abrarshakhi.dourdiary.features.tracking.data.session.RunSessionManager
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunSnapshot
import com.abrarshakhi.dourdiary.features.tracking.domain.repository.RunTracker
import com.abrarshakhi.dourdiary.features.tracking.service.RunTrackingService
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow

class ServiceRunTracker(
    private val context: Context,
    private val sessionManager: RunSessionManager,
) : RunTracker {

    override val snapshot: StateFlow<RunSnapshot> = sessionManager.snapshot

    override val activeRunId: StateFlow<Long?> = sessionManager.activeRunId

    override val route: StateFlow<List<GeoPoint>> = sessionManager.route

    override val recoverableRun: StateFlow<Run?> = sessionManager.recoverableRun

    override val finishedRuns: SharedFlow<Long> = sessionManager.finishedRuns

    override fun start() = command(RunTrackingService.ActionStart)

    override fun pause() = command(RunTrackingService.ActionPause)

    override fun resume() = command(RunTrackingService.ActionResume)

    override fun stop() = command(RunTrackingService.ActionStop)

    override suspend fun checkForRecoverableRun() = sessionManager.checkForRecoverableRun()

    override fun resumeRecoveredRun() = command(RunTrackingService.ActionResumeRecovered)

    override suspend fun discardRecoveredRun() = sessionManager.discardRecoveredRun()

    private fun command(action: String) = RunTrackingService.command(context, action)
}
