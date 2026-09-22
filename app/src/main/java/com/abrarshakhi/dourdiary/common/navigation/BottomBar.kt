package com.abrarshakhi.dourdiary.common.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.automirrored.outlined.DirectionsRun
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.abrarshakhi.dourdiary.R

enum class BottomBarDestination(
    val route: AppRouteKey,
    @param:StringRes val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
) {
    Home(
        route = AppRouteKey.Home,
        labelRes = R.string.nav_home,
        selectedIcon = Icons.AutoMirrored.Filled.DirectionsRun,
        unselectedIcon = Icons.AutoMirrored.Outlined.DirectionsRun,
    ),
    History(
        route = AppRouteKey.History,
        labelRes = R.string.nav_history,
        selectedIcon = Icons.Filled.History,
        unselectedIcon = Icons.Outlined.History,
    ),
    Settings(
        route = AppRouteKey.Settings,
        labelRes = R.string.nav_settings,
        selectedIcon = Icons.Filled.Settings,
        unselectedIcon = Icons.Outlined.Settings,
    ),
}

@Composable
fun BottomBar(
    selected: BottomBarKey,
    backStack: SnapshotStateList<AppRouteKey>,
) {
    NavigationBar {
        BottomBarDestination.entries.forEach { destination ->
            val isSelected = destination.route == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { if (!isSelected) backStack.switchTabTo(destination.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) destination.selectedIcon else destination.unselectedIcon,
                        contentDescription = null,
                    )
                },
                label = { Text(text = stringResource(destination.labelRes)) },
            )
        }
    }
}
