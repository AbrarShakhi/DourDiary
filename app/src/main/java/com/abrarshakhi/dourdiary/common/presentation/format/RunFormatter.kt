package com.abrarshakhi.dourdiary.common.presentation.format

import com.abrarshakhi.dourdiary.common.domain.model.Distance
import com.abrarshakhi.dourdiary.common.domain.model.Pace
import com.abrarshakhi.dourdiary.common.domain.model.UnitSystem
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlin.math.roundToLong

object RunFormatter {

    const val PlaceholderPace = "--:--"

    fun distance(
        distance: Distance,
        unitSystem: UnitSystem,
        locale: Locale = Locale.getDefault(),
    ): String = String.format(locale, "%.2f", distance.inUnit(unitSystem))

    fun duration(millis: Long, locale: Locale = Locale.getDefault()): String {
        val totalSeconds = (millis.coerceAtLeast(0L)) / 1_000L
        val hours = totalSeconds / 3_600L
        val minutes = (totalSeconds % 3_600L) / 60L
        val seconds = totalSeconds % 60L

        return if (hours > 0L) {
            String.format(locale, "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(locale, "%d:%02d", minutes, seconds)
        }
    }

    fun dateTime(
        epochMillis: Long,
        locale: Locale = Locale.getDefault(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): String = DateTimeFormatter
        .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(locale)
        .format(Instant.ofEpochMilli(epochMillis).atZone(zone))

    fun pace(
        pace: Pace?,
        unitSystem: UnitSystem,
        locale: Locale = Locale.getDefault(),
    ): String {
        if (pace == null) return PlaceholderPace
        val totalSeconds = pace.secondsPerUnit(unitSystem).roundToLong()
        if (totalSeconds >= 3_600L) return PlaceholderPace
        return String.format(locale, "%d:%02d", totalSeconds / 60L, totalSeconds % 60L)
    }
}
