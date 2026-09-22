package com.abrarshakhi.dourdiary.common.domain.model

enum class UnitSystem {
    METRIC, IMPERIAL, ;

    val cueUnit: Distance
        get() = when (this) {
            METRIC -> Distance.ofKilometers(1.0)
            IMPERIAL -> Distance.ofMiles(1.0)
        }
}
