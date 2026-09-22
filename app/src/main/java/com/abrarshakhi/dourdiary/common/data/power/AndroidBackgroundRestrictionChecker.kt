package com.abrarshakhi.dourdiary.common.data.power

import android.content.Context
import android.os.PowerManager
import androidx.core.content.getSystemService
import com.abrarshakhi.dourdiary.common.domain.power.BackgroundRestrictionChecker

class AndroidBackgroundRestrictionChecker(
    private val context: Context,
) : BackgroundRestrictionChecker {
    override fun isExemptFromBatteryOptimisation(): Boolean =
        context.getSystemService<PowerManager>()
            ?.isIgnoringBatteryOptimizations(context.packageName)
            ?: true
}
