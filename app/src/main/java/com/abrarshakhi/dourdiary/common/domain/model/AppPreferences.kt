package com.abrarshakhi.dourdiary.common.domain.model

data class AppPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val dynamicColor: Boolean = true,
)
