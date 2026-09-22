package com.abrarshakhi.dourdiary.features.tracking.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.core.content.ContextCompat
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.features.tracking.domain.datasource.LocationDataSource
import com.abrarshakhi.dourdiary.features.tracking.domain.model.LocationSample
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class FusedLocationDataSource(
    private val context: Context,
) : LocationDataSource {

    private val client = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override fun locationUpdates(intervalMillis: Long): Flow<LocationSample> = callbackFlow {
        if (!hasLocationPermission()) {
            close(SecurityException("Location permission is not granted"))
            return@callbackFlow
        }

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, intervalMillis)
            .setMinUpdateIntervalMillis(intervalMillis)
            .setMinUpdateDistanceMeters(0f)
            .setWaitForAccurateLocation(false)
            .build()

        val callback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location -> trySend(location.toSample()) }
            }
        }

        client.requestLocationUpdates(request, callback, Looper.getMainLooper())

        awaitClose { client.removeLocationUpdates(callback) }
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED

    private fun Location.toSample(): LocationSample = LocationSample(
        point = GeoPoint(
            latitude = latitude,
            longitude = longitude,
            altitudeMeters = if (hasAltitude()) altitude else null,
        ),
        accuracyMeters = if (hasAccuracy()) accuracy.toDouble() else Double.MAX_VALUE,
        elapsedRealtimeMillis = elapsedRealtimeNanos / 1_000_000L,
        epochMillis = time,
        speedMetersPerSecond = if (hasSpeed()) speed.toDouble() else null,
    )
}
