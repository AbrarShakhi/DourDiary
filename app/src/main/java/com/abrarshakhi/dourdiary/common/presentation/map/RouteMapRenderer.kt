package com.abrarshakhi.dourdiary.common.presentation.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint

enum class MapCamera {
    FIT_ROUTE,

    FOLLOW_HEAD,
}

interface RouteMapRenderer {

    @Composable
    fun Map(
        points: List<GeoPoint>,
        camera: MapCamera,
        modifier: Modifier,
        showCurrentPosition: Boolean,
        overlayBottomPadding: Dp,
    )
}
