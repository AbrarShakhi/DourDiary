package com.abrarshakhi.dourdiary.features.tracking.service

import android.Manifest
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.abrarshakhi.dourdiary.common.domain.repository.AppPreferencesRepository
import com.abrarshakhi.dourdiary.features.tracking.data.session.RunSessionManager
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import org.koin.android.ext.android.inject

class RunTrackingService : Service() {

    private val sessionManager: RunSessionManager by inject()
    private val notifications: TrackingNotifications by inject()
    private val preferences: AppPreferencesRepository by inject()
    private val cuePlayer: RunCuePlayer by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        notifications.ensureChannel()
        cuePlayer.initialise()
        observeRunForNotification()
        observeCues()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ActionStart -> if (startRecordingAllowed()) {
                stopping = false
                enterForeground()
                sessionManager.start()
            }

            ActionResumeRecovered -> if (startRecordingAllowed()) {
                stopping = false
                enterForeground()
                sessionManager.resumeRecoveredRun()
            }

            ActionPause -> sessionManager.pause()

            ActionResume -> sessionManager.resume()

            ActionStop -> {
                stopping = true
                sessionManager.stop()
                ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
                notifications.cancel()
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    override fun onDestroy() {
        serviceScope.cancel()
        notifications.cancel()
        cuePlayer.release()
        super.onDestroy()
    }

    private fun observeCues() {
        sessionManager.cues
            .onEach { cue -> cuePlayer.play(cue, currentUnitSystem) }
            .launchIn(serviceScope)
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun enterForeground() {
        ServiceCompat.startForeground(
            this,
            TrackingNotifications.NotificationId,
            notifications.build(sessionManager.snapshot.value, currentUnitSystem),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
        )
    }

    private fun startRecordingAllowed(): Boolean {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION,
        ) == PackageManager.PERMISSION_GRANTED

        if (!granted) stopSelf()
        return granted
    }

    @Volatile
    private var currentUnitSystem = UnitSystem.METRIC

    @Volatile
    private var stopping = false

    private fun observeRunForNotification() {
        combine(
            sessionManager.snapshot,
            preferences.preferences.map { it.unitSystem },
        ) { snapshot, unitSystem -> snapshot to unitSystem }
            .onEach { (snapshot, unitSystem) ->
                currentUnitSystem = unitSystem
                if (!stopping && snapshot.status.isRecording) {
                    notifications.notify(notifications.build(snapshot, unitSystem))
                }
            }
            .launchIn(serviceScope)
    }

    companion object {
        const val ActionStart = "com.abrarshakhi.dourdiary.action.START"
        const val ActionPause = "com.abrarshakhi.dourdiary.action.PAUSE"
        const val ActionResume = "com.abrarshakhi.dourdiary.action.RESUME"
        const val ActionStop = "com.abrarshakhi.dourdiary.action.STOP"
        const val ActionResumeRecovered = "com.abrarshakhi.dourdiary.action.RESUME_RECOVERED"

        fun command(context: Context, action: String) {
            val intent = Intent(context, RunTrackingService::class.java).setAction(action)
            if (action == ActionStart || action == ActionResumeRecovered) {
                ContextCompat.startForegroundService(context, intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
