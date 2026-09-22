package com.abrarshakhi.dourdiary.common.presentation.map

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import kotlinx.coroutines.suspendCancellableCoroutine
import org.maplibre.android.MapLibre
import com.abrarshakhi.dourdiary.common.domain.geo.RouteSimplifier
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.location.LocationComponentActivationOptions
import org.maplibre.android.location.modes.CameraMode
import org.maplibre.android.location.modes.RenderMode
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.maps.Style
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.coroutines.resume

class MapLibreRouteMapRenderer : RouteMapRenderer {

    @Composable
    override fun Map(
        points: List<GeoPoint>,
        camera: MapCamera,
        modifier: Modifier,
        showCurrentPosition: Boolean,
        overlayBottomPadding: Dp,
    ) {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val styleUrl = MapStyles.forTheme(isDark)
        val routeColor = MaterialTheme.colorScheme.primary.toArgb()

        val context = LocalContext.current
        val mapView = rememberMapViewWithLifecycle()
        var style by remember { mutableStateOf<Style?>(null) }
        var map by remember { mutableStateOf<MapLibreMap?>(null) }

        val drawnPoints = remember(points) { RouteSimplifier.sample(points, MaxDrawnPoints) }

        val density = LocalDensity.current
        val edgePaddingPx = with(density) { MapEdgePadding.roundToPx() }
        val overlayPaddingPx = with(density) { overlayBottomPadding.roundToPx() }

        AndroidView(factory = { mapView }, modifier = modifier)

        LaunchedEffect(mapView, styleUrl, showCurrentPosition) {
            style = null
            val loadedMap = mapView.awaitMap()
            loadedMap.uiSettings.apply {
                isRotateGesturesEnabled = false
                isTiltGesturesEnabled = false
                isAttributionEnabled = true
                isLogoEnabled = true
                val bottom = edgePaddingPx + overlayPaddingPx
                setLogoMargins(edgePaddingPx, 0, 0, bottom)
                setAttributionMargins(edgePaddingPx + LogoWidthPx, 0, 0, bottom)
            }
            map = loadedMap
            val loadedStyle = loadedMap.awaitStyle(styleUrl)
            if (showCurrentPosition) loadedMap.showUserLocation(context, loadedStyle)
            style = loadedStyle
        }

        LaunchedEffect(style, drawnPoints, routeColor) {
            val loadedStyle = style ?: return@LaunchedEffect
            loadedStyle.drawRoute(drawnPoints, routeColor)
        }

        LaunchedEffect(map, style, drawnPoints, camera, showCurrentPosition) {
            val loadedMap = map ?: return@LaunchedEffect
            if (style == null) return@LaunchedEffect
            loadedMap.moveCameraFor(drawnPoints, camera, showCurrentPosition)
        }
    }
}

private const val RouteSourceId = "dour-diary-route"
private const val RouteLayerId = "dour-diary-route-line"

private fun Style.drawRoute(points: List<GeoPoint>, colorArgb: Int) {
    val line = LineString.fromLngLats(
        points.map { Point.fromLngLat(it.longitude, it.latitude) },
    )

    val existing = getSourceAs<GeoJsonSource>(RouteSourceId)
    if (existing != null) {
        existing.setGeoJson(line)
        return
    }

    addSource(GeoJsonSource(RouteSourceId, line))
    addLayer(
        LineLayer(RouteLayerId, RouteSourceId).withProperties(
            PropertyFactory.lineColor(colorArgb),
            PropertyFactory.lineWidth(5f),
            PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
            PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
        ),
    )
}

@SuppressLint("MissingPermission")
private fun MapLibreMap.moveCameraFor(
    points: List<GeoPoint>,
    camera: MapCamera,
    showCurrentPosition: Boolean,
) {
    val positions = points.map { LatLng(it.latitude, it.longitude) }

    when {
        positions.size >= 2 && camera == MapCamera.FIT_ROUTE -> {
            locationComponent.takeIf { it.isLocationComponentActivated }?.cameraMode =
                CameraMode.NONE
            val bounds = runCatching {
                LatLngBounds.Builder().includes(positions).build()
            }.getOrNull() ?: return

            val fitted = getCameraForLatLngBounds(
                bounds,
                intArrayOf(RouteFitPaddingPx, RouteFitPaddingPx, RouteFitPaddingPx, RouteFitPaddingPx),
            )
            if (fitted == null) {
                easeCamera(CameraUpdateFactory.newLatLngBounds(bounds, RouteFitPaddingPx))
            } else {
                val clamped = CameraPosition.Builder(fitted)
                    .zoom(minOf(fitted.zoom, MaxFitZoom))
                    .build()
                easeCamera(CameraUpdateFactory.newCameraPosition(clamped))
            }
        }

        positions.isNotEmpty() -> {
            locationComponent.takeIf { it.isLocationComponentActivated }?.cameraMode =
                CameraMode.NONE
            easeCamera(CameraUpdateFactory.newLatLngZoom(positions.last(), RunningZoom))
        }

        showCurrentPosition -> {
            val component = locationComponent.takeIf { it.isLocationComponentActivated } ?: return
            component.lastKnownLocation?.let { location ->
                moveCamera(
                    CameraUpdateFactory.newLatLngZoom(
                        LatLng(location.latitude, location.longitude),
                        RunningZoom,
                    ),
                )
            }
            component.cameraMode = CameraMode.TRACKING
            component.zoomWhileTracking(RunningZoom)
        }
    }
}

@SuppressLint("MissingPermission")
private fun MapLibreMap.showUserLocation(context: Context, style: Style) {
    val granted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    if (!granted) return

    runCatching {
        locationComponent.activateLocationComponent(
            LocationComponentActivationOptions.builder(context, style).build(),
        )
        locationComponent.isLocationComponentEnabled = true
        locationComponent.renderMode = RenderMode.NORMAL
    }
}

private const val RouteFitPaddingPx = 96
private const val RunningZoom = 16.0
private const val MaxFitZoom = 16.5

private const val MaxDrawnPoints = 1_000
private val MapEdgePadding = 8.dp

private const val LogoWidthPx = 190

@Composable
private fun rememberMapViewWithLifecycle(): MapView {
    val context = LocalContext.current
    val mapView = remember(context) { createMapView(context) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val destroyed = remember(mapView) { booleanArrayOf(false) }

    DisposableEffect(lifecycle, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            if (destroyed[0]) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_START -> mapView.onStart()
                Lifecycle.Event.ON_RESUME -> mapView.onResume()
                Lifecycle.Event.ON_PAUSE -> mapView.onPause()
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)

        onDispose {
            lifecycle.removeObserver(observer)
            if (!destroyed[0]) {
                destroyed[0] = true
                mapView.onStop()
                mapView.onDestroy()
            }
        }
    }

    return mapView
}

private fun createMapView(context: Context): MapView {
    MapLibre.getInstance(context)
    return MapView(context).apply { onCreate(null) }
}

private suspend fun MapView.awaitMap(): MapLibreMap =
    suspendCancellableCoroutine { continuation ->
        getMapAsync { map -> continuation.resume(map) }
    }

private suspend fun MapLibreMap.awaitStyle(styleUrl: String): Style =
    suspendCancellableCoroutine { continuation ->
        setStyle(styleUrl) { style -> continuation.resume(style) }
    }
