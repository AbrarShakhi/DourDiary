package com.abrarshakhi.dourdiary.features.tracking.data.location

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.abrarshakhi.dourdiary.features.tracking.domain.LocationPermissionChecker

class AndroidLocationPermissionChecker(
    private val context: Context,
) : LocationPermissionChecker {
    override fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) ==
            PackageManager.PERMISSION_GRANTED
}
