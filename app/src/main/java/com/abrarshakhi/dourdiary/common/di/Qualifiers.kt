package com.abrarshakhi.dourdiary.common.di

import org.koin.core.qualifier.named

val DataStoreScope = named("dataStoreScope")

val IoDispatcher = named("ioDispatcher")

val ApplicationScope = named("applicationScope")
