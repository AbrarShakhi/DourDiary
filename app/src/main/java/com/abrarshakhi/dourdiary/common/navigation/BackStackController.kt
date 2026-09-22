package com.abrarshakhi.dourdiary.common.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlinx.serialization.json.Json

fun <T> SnapshotStateList<T>.currentRoute(): T? = lastOrNull()

fun <T> SnapshotStateList<T>.switchTabTo(destination: T) {
    clear()
    add(destination)
}

fun <T> SnapshotStateList<T>.back(): Boolean {
    if (size <= 1) return false
    removeAt(lastIndex)
    return true
}

fun <T> SnapshotStateList<T>.navigateTo(destination: T) {
    add(destination)
}

private val AppRouteBackStackSaver: Saver<SnapshotStateList<AppRouteKey>, Any> = listSaver(
    save = { stack -> stack.map { Json.encodeToString<AppRouteKey>(it) } },
    restore = { saved ->
        val routes = saved.mapNotNull { encoded ->
            runCatching { Json.decodeFromString<AppRouteKey>(encoded) }.getOrNull()
        }
        mutableStateListOf<AppRouteKey>().apply {
            addAll(routes.ifEmpty { listOf(AppRouteKey.Home) })
        }
    },
)

@Composable
fun rememberAppBackStack(start: AppRouteKey = AppRouteKey.Home): SnapshotStateList<AppRouteKey> =
    rememberSaveable(saver = AppRouteBackStackSaver) {
        mutableStateListOf(start)
    }
