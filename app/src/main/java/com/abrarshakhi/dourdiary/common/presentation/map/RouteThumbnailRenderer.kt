package com.abrarshakhi.dourdiary.common.presentation.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.abrarshakhi.dourdiary.common.domain.model.GeoPoint

interface RouteThumbnailRenderer {

    @Composable
    fun Thumbnail(
        points: List<GeoPoint>,
        routeId: String,
        modifier: Modifier,
    )
}
