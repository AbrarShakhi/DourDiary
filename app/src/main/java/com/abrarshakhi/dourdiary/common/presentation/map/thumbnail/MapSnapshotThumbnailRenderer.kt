package com.abrarshakhi.dourdiary.common.presentation.map.thumbnail

import android.content.Context
import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint
import com.abrarshakhi.dourdiary.common.presentation.map.MapStyles
import com.abrarshakhi.dourdiary.common.presentation.map.RouteThumbnailRenderer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.maplibre.android.MapLibre
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.Property
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import kotlin.coroutines.resume

class MapSnapshotThumbnailRenderer(
    private val context: Context,
    private val cache: RouteThumbnailCache,
    private val fallback: RouteThumbnailRenderer,
) : RouteThumbnailRenderer {

    @Composable
    override fun Thumbnail(points: List<GeoPoint>, routeId: String, modifier: Modifier) {
        val isDark = MaterialTheme.colorScheme.surface.luminance() < 0.5f
        val routeColor = MaterialTheme.colorScheme.primary.toArgb()
        val appContext = LocalContext.current.applicationContext

        BoxWithConstraints(modifier) {
            val widthPx = constraints.maxWidth
            val heightPx = constraints.maxHeight
            val key = remember(routeId, isDark, widthPx, heightPx) {
                thumbnailKey(routeId, isDark, widthPx, heightPx)
            }

            var snapshot by remember(key) { mutableStateOf(cache.peek(key)) }

            LaunchedEffect(key, points) {
                if (snapshot != null) return@LaunchedEffect
                if (points.size < 2 || widthPx <= 0 || heightPx <= 0) return@LaunchedEffect

                snapshot = cache.getOrRender(key) {
                    renderRouteSnapshot(
                        context = appContext,
                        points = points,
                        widthPx = widthPx,
                        heightPx = heightPx,
                        styleUrl = MapStyles.forTheme(isDark),
                        routeColorArgb = routeColor,
                    )
                }
            }

            val rendered = snapshot
            if (rendered == null) {
                fallback.Thumbnail(points, routeId, Modifier.fillMaxSize())
            } else {
                Image(
                    bitmap = rendered.asImageBitmap(),
                    contentDescription = null,
                    contentScale = ContentScale.FillBounds,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

private suspend fun renderRouteSnapshot(
    context: Context,
    points: List<GeoPoint>,
    widthPx: Int,
    heightPx: Int,
    styleUrl: String,
    routeColorArgb: Int,
): Bitmap? = withContext(Dispatchers.Main) {
    MapLibre.getInstance(context)

    val positions = points.map { LatLng(it.latitude, it.longitude) }
    val bounds = runCatching { LatLngBounds.Builder().includes(positions).build() }.getOrNull()
        ?: return@withContext null

    val styleBuilder = Style.Builder()
        .fromUri(styleUrl)
        .withSource(
            GeoJsonSource(
                SnapshotRouteSourceId,
                LineString.fromLngLats(points.map { Point.fromLngLat(it.longitude, it.latitude) }),
            ),
        )
        .withLayer(
            LineLayer(SnapshotRouteLayerId, SnapshotRouteSourceId).withProperties(
                PropertyFactory.lineColor(routeColorArgb),
                PropertyFactory.lineWidth(SnapshotLineWidth),
                PropertyFactory.lineCap(Property.LINE_CAP_ROUND),
                PropertyFactory.lineJoin(Property.LINE_JOIN_ROUND),
            ),
        )

    val options = MapSnapshotter.Options(widthPx, heightPx)
        .withStyleBuilder(styleBuilder)
        .withRegion(bounds)
        .withPixelRatio(1f)

    suspendCancellableCoroutine { continuation ->
        val snapshotter = MapSnapshotter(context, options)
        continuation.invokeOnCancellation { runCatching { snapshotter.cancel() } }
        snapshotter.start(
            { result -> if (continuation.isActive) continuation.resume(result.bitmap) },
            { _ -> if (continuation.isActive) continuation.resume(null) },
        )
    }
}

private const val SnapshotRouteSourceId = "thumbnail-route"
private const val SnapshotRouteLayerId = "thumbnail-route-line"
private const val SnapshotLineWidth = 4f
