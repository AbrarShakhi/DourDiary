package com.abrarshakhi.dourdiary.common.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = Typography()

val Typography = Default.copy(
    displayLarge = Default.displayLarge.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 64.sp,
        lineHeight = 68.sp,
        letterSpacing = (-2).sp,
    ),
    displayMedium = Default.displayMedium.copy(
        fontFamily = FontFamily.Monospace,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 50.sp,
        letterSpacing = (-1.5).sp,
    ),
    headlineSmall = Default.headlineSmall.copy(
        fontWeight = FontWeight.SemiBold,
    ),
    titleMedium = Default.titleMedium.copy(
        fontWeight = FontWeight.SemiBold,
    ),
    labelMedium = Default.labelMedium.copy(
        letterSpacing = 0.6.sp,
    ),
)

val StatNumber: TextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Bold,
)
