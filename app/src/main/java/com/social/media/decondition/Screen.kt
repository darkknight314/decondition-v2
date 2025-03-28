package com.decondition

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(val route: String, val title: String, val icon: ImageVector) {
    object Apps : Screen("apps", "Apps", Icons.Default.Apps)
    object Domains : Screen("domains", "Domains", Icons.Default.Language)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
}