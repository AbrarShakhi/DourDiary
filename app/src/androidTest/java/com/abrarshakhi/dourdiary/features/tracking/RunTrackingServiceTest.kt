package com.abrarshakhi.dourdiary.features.tracking

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.content.getSystemService
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.abrarshakhi.dourdiary.common.domain.repository.RunRepository
import com.abrarshakhi.dourdiary.features.tracking.domain.repository.RunTracker
import com.abrarshakhi.dourdiary.features.tracking.service.RunTrackingService
import com.abrarshakhi.dourdiary.features.tracking.service.TrackingNotifications
import kotlinx.coroutines.runBlocking
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext

@RunWith(AndroidJUnit4::class)
class RunTrackingServiceTest {

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    @Before
    fun requireLocationPermission() {
        runCatching {
            InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant ${'$'}{context.packageName} ${'$'}{Manifest.permission.ACCESS_FINE_LOCATION}",
            ).close()
            Thread.sleep(500L)
        }

        assumeTrue(
            "Location permission is not granted and this device will not grant it from a test. " +
                "Grant it to the app in system settings to run this test.",
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
                PackageManager.PERMISSION_GRANTED,
        )
    }

    private val tracker: RunTracker by lazy { GlobalContext.get().get() }
    private val runRepository: RunRepository by lazy { GlobalContext.get().get() }

    private var startedRunId: Long? = null

    @After
    fun tearDown() = runBlocking {
        context.startService(
            Intent(context, RunTrackingService::class.java)
                .setAction(RunTrackingService.ActionStop),
        )
        waitUntil("the service to release the run") { tracker.activeRunId.value == null }
        startedRunId?.let { runRepository.deleteRun(it) }
        Unit
    }

    @Test
    fun starting_the_service_begins_a_run_and_shows_an_ongoing_notification() {
        tracker.start()

        assertTrue(
            "The service never took ownership of a run",
            waitUntil("a run to start") { tracker.activeRunId.value != null },
        )
        startedRunId = tracker.activeRunId.value

        assertTrue(
            "No foreground notification was posted",
            waitUntil("the notification") { trackingNotification() != null },
        )

        val notification = trackingNotification()!!
        assertTrue(
            "The recording notification must not be dismissable",
            notification.notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0,
        )
    }

    @Test
    fun the_started_run_is_immediately_recoverable_from_disk() = runBlocking {
        tracker.start()
        waitUntil("a run to start") { tracker.activeRunId.value != null }
        startedRunId = tracker.activeRunId.value

        val inProgress = runRepository.findInProgressRun()

        assertNotNull("A crash at this moment would have lost the run", inProgress)
        assertTrue(inProgress!!.isComplete.not())
    }

    @Test
    fun stopping_the_service_completes_the_run_and_clears_the_notification() = runBlocking {
        tracker.start()
        waitUntil("a run to start") { tracker.activeRunId.value != null }
        val runId = tracker.activeRunId.value!!
        startedRunId = runId
        waitUntil("the notification") { trackingNotification() != null }

        tracker.stop()

        assertTrue(
            "The run never finished",
            waitUntil("the run to finish") { tracker.activeRunId.value == null },
        )
        assertTrue(runRepository.getRun(runId)!!.isComplete)
        assertNull("A finished run is still offered for recovery", runRepository.findInProgressRun())
        assertTrue(
            "The notification outlived the run",
            waitUntil("the notification to clear") { trackingNotification() == null },
        )
    }

    private fun trackingNotification() =
        context.getSystemService<NotificationManager>()
            ?.activeNotifications
            ?.firstOrNull { it.id == TrackingNotifications.NotificationId }

    private fun waitUntil(
        what: String,
        timeoutMillis: Long = 10_000L,
        condition: () -> Boolean,
    ): Boolean {
        val deadline = System.currentTimeMillis() + timeoutMillis
        while (System.currentTimeMillis() < deadline) {
            if (condition()) return true
            Thread.sleep(50L)
        }
        return condition()
    }
}
