package com.abrarshakhi.dourdiary.features.settings.presentation

import androidx.compose.runtime.Immutable

@Immutable
data class OpenSourceComponent(
    val name: String,
    val license: String,
    val copyright: String,
)

val OpenSourceComponents: List<OpenSourceComponent> = listOf(
    OpenSourceComponent(
        name = "AndroidX (Core, Activity, Compose, Lifecycle, Navigation 3, Room, DataStore)",
        license = "Apache License 2.0",
        copyright = "The Android Open Source Project",
    ),
    OpenSourceComponent(
        name = "Jetpack Compose, Material 3 and Material icons",
        license = "Apache License 2.0",
        copyright = "The Android Open Source Project",
    ),
    OpenSourceComponent(
        name = "Kotlin standard library",
        license = "Apache License 2.0",
        copyright = "JetBrains s.r.o. and Kotlin Programming Language contributors",
    ),
    OpenSourceComponent(
        name = "kotlinx.coroutines and kotlinx.serialization",
        license = "Apache License 2.0",
        copyright = "JetBrains s.r.o.",
    ),
    OpenSourceComponent(
        name = "Koin",
        license = "Apache License 2.0",
        copyright = "Kotzilla and Koin contributors",
    ),
    OpenSourceComponent(
        name = "MapLibre Native for Android",
        license = "BSD 2-Clause \"Simplified\" License",
        copyright = "MapLibre contributors",
    ),
    OpenSourceComponent(
        name = "OkHttp",
        license = "Apache License 2.0",
        copyright = "Square, Inc.",
    ),
    OpenSourceComponent(
        name = "Timber",
        license = "Apache License 2.0",
        copyright = "Jake Wharton",
    ),
    OpenSourceComponent(
        name = "Google Play services (Location)",
        license = "Android Software Development Kit License",
        copyright = "Google LLC",
    ),
)
