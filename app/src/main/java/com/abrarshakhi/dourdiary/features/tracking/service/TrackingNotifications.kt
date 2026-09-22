package com.abrarshakhi.dourdiary.features.tracking.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.abrarshakhi.dourdiary.R
import com.abrarshakhi.dourdiary.common.MainActivity
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import com.abrarshakhi.dourdiary.common.presentation.format.RunFormatter
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunSnapshot
import com.abrarshakhi.dourdiary.features.tracking.domain.model.RunStatus

class TrackingNotifications(private val context: Context) {

    private val manager = context.getSystemService<NotificationManager>()

    fun ensureChannel() {
        val channel = NotificationChannel(
            ChannelId,
            context.getString(R.string.tracking_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = context.getString(R.string.tracking_channel_description)
            setShowBadge(false)
        }
        manager?.createNotificationChannel(channel)
    }

    fun build(snapshot: RunSnapshot, unitSystem: UnitSystem): Notification {
        val unitLabel = context.getString(
            when (unitSystem) {
                UnitSystem.METRIC -> R.string.unit_kilometers_short
                UnitSystem.IMPERIAL -> R.string.unit_miles_short
            },
        )
        val distance = RunFormatter.distance(snapshot.distance, unitSystem)
        val duration = RunFormatter.duration(snapshot.movingDurationMillis)
        val pace = RunFormatter.pace(snapshot.averagePace, unitSystem)

        val builder = NotificationCompat.Builder(context, ChannelId)
            .setSmallIcon(R.drawable.ic_notification_run)
            .setContentTitle(
                context.getString(
                    when (snapshot.status) {
                        RunStatus.PAUSED -> R.string.tracking_notification_paused
                        RunStatus.AUTO_PAUSED -> R.string.tracking_notification_auto_paused
                        else -> R.string.tracking_notification_recording
                    },
                ),
            )
            .setContentText(
                context.getString(
                    R.string.tracking_notification_summary,
                    distance,
                    unitLabel,
                    duration,
                    pace,
                ),
            )
            .setContentIntent(openAppIntent())
            .setOngoing(true)
            .setSilent(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_WORKOUT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)

        if (snapshot.status.isPaused) {
            builder.addAction(
                0,
                context.getString(R.string.tracking_action_resume),
                serviceIntent(RunTrackingService.ActionResume),
            )
        } else {
            builder.addAction(
                0,
                context.getString(R.string.tracking_action_pause),
                serviceIntent(RunTrackingService.ActionPause),
            )
        }
        builder.addAction(
            0,
            context.getString(R.string.tracking_action_stop),
            serviceIntent(RunTrackingService.ActionStop),
        )

        return builder.build()
    }

    fun notify(notification: Notification) {
        manager?.notify(NotificationId, notification)
    }

    fun cancel() {
        manager?.cancel(NotificationId)
    }

    private fun openAppIntent(): PendingIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(context, MainActivity::class.java)
            .setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
        PendingIntent.FLAG_IMMUTABLE,
    )

    private fun serviceIntent(action: String): PendingIntent = PendingIntent.getService(
        context,
        action.hashCode(),
        Intent(context, RunTrackingService::class.java).setAction(action),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    companion object {
        const val ChannelId = "run_tracking"
        const val NotificationId = 1001
    }
}
