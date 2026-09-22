package com.abrarshakhi.dourdiary.features.tracking.domain.datasource

import com.abrarshakhi.dourdiary.features.tracking.domain.model.LocationSample
import kotlinx.coroutines.flow.Flow

interface LocationDataSource {
    fun locationUpdates(intervalMillis: Long): Flow<LocationSample>
}
