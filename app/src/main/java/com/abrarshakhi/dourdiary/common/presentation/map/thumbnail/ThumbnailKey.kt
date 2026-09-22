package com.abrarshakhi.dourdiary.common.presentation.map.thumbnail

internal fun thumbnailKey(
    routeId: String,
    isDark: Boolean,
    widthPx: Int,
    heightPx: Int,
): String {
    val theme = if (isDark) "dark" else "light"
    return "v$ThumbnailFormatVersion-$routeId-$theme-${bucket(widthPx)}x${bucket(heightPx)}"
}

internal fun bucket(pixels: Int, size: Int = SizeBucketPx): Int {
    if (pixels <= 0) return 0
    return ((pixels + size - 1) / size) * size
}

private const val ThumbnailFormatVersion = 1
private const val SizeBucketPx = 32
