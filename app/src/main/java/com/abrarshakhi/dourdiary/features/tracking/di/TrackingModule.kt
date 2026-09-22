package com.abrarshakhi.dourdiary.features.tracking.di

import com.abrarshakhi.dourdiary.common.di.ApplicationScope
import com.abrarshakhi.dourdiary.features.tracking.data.ServiceRunTracker
import com.abrarshakhi.dourdiary.common.di.IoDispatcher
import com.abrarshakhi.dourdiary.common.presentation.map.CanvasRouteThumbnailRenderer
import com.abrarshakhi.dourdiary.common.presentation.map.MapLibreRouteMapRenderer
import com.abrarshakhi.dourdiary.common.presentation.map.RouteMapRenderer
import com.abrarshakhi.dourdiary.common.presentation.map.RouteThumbnailRenderer
import com.abrarshakhi.dourdiary.common.presentation.map.thumbnail.MapSnapshotThumbnailRenderer
import com.abrarshakhi.dourdiary.common.presentation.map.thumbnail.RouteThumbnailCache
import java.io.File
import com.abrarshakhi.dourdiary.features.tracking.data.location.AndroidLocationPermissionChecker
import com.abrarshakhi.dourdiary.features.tracking.data.location.AndroidTrackingClock
import com.abrarshakhi.dourdiary.features.tracking.data.location.FusedLocationDataSource
import com.abrarshakhi.dourdiary.features.tracking.data.session.RunSessionManager
import com.abrarshakhi.dourdiary.features.tracking.domain.TrackingClock
import com.abrarshakhi.dourdiary.features.tracking.domain.LocationPermissionChecker
import com.abrarshakhi.dourdiary.features.tracking.domain.datasource.LocationDataSource
import com.abrarshakhi.dourdiary.features.tracking.domain.engine.RunEngine
import com.abrarshakhi.dourdiary.features.tracking.domain.model.TrackingConfig
import com.abrarshakhi.dourdiary.features.tracking.domain.repository.RunTracker
import com.abrarshakhi.dourdiary.features.tracking.service.RunCuePlayer
import com.abrarshakhi.dourdiary.features.tracking.service.TrackingNotifications
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val trackingModule = module {
    single { TrackingConfig() }

    single<TrackingClock> { AndroidTrackingClock() }

    single<LocationDataSource> { FusedLocationDataSource(androidContext()) }

    single<LocationPermissionChecker> { AndroidLocationPermissionChecker(androidContext()) }

    single<RouteMapRenderer> { MapLibreRouteMapRenderer() }

    single {
        RouteThumbnailCache(
            directory = File(androidContext().cacheDir, "route-thumbnails"),
            ioDispatcher = get(IoDispatcher),
        )
    }

    single<RouteThumbnailRenderer> {
        MapSnapshotThumbnailRenderer(
            context = androidContext(),
            cache = get(),
            fallback = CanvasRouteThumbnailRenderer(),
        )
    }

    single { RunEngine(config = get()) }

    single { TrackingNotifications(androidContext()) }

    single { RunCuePlayer(androidContext()) }

    single {
        RunSessionManager(
            locationDataSource = get(),
            runRepository = get(),
            preferencesRepository = get(),
            clock = get(),
            engine = get(),
            scope = get(ApplicationScope),
            config = get(),
        )
    }

    single<RunTracker> { ServiceRunTracker(context = androidContext(), sessionManager = get()) }
}
