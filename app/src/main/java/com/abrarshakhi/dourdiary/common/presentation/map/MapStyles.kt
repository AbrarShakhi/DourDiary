package com.abrarshakhi.dourdiary.common.presentation.map

object MapStyles {

    const val LIGHT = "https://tiles.openfreemap.org/styles/positron"
    const val DARK = "https://tiles.openfreemap.org/styles/dark"

    fun forTheme(isDark: Boolean): String = if (isDark) DARK else LIGHT
}
