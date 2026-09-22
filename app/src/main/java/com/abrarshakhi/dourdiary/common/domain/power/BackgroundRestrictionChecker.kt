package com.abrarshakhi.dourdiary.common.domain.power

interface BackgroundRestrictionChecker {
    fun isExemptFromBatteryOptimisation(): Boolean
}
