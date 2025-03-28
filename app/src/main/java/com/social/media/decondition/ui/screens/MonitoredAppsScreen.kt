package com.decondition.ui.screens.apps

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MonitoredAppsScreen(navController: NavController) {
    // In a real app, we would get this from a ViewModel
    val sampleApps = remember {
        listOf(
            "Instagram" to "com.instagram.android",
            "Facebook" to "com.facebook.katana",
            "TikTok" to "com.zhiliaoapp.musically"
        )
    }

    var showUnmonitoredApps by remember { mutableStateOf(false) }
    var selectedApp by remember { mutableStateOf<Pair<String, String>?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (sampleApps.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No apps being monitored",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Tap + to add apps you want to monitor",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // List of monitored apps
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleApps) { (appName, packageName) ->
                    AppItem(appName, packageName) {
                        selectedApp = appName to packageName
                    }
                }
            }
        }

        // Floating action button to add new apps
        FloatingActionButton(
            onClick = { showUnmonitoredApps = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }

    // Dialog to confirm removing app from monitoring
    if (selectedApp != null) {
        AlertDialog(
            onDismissRequest = { selectedApp = null },
            title = { Text("Stop Monitoring?") },
            text = { Text("Do you want to stop monitoring ${selectedApp?.first}?") },
            confirmButton = {
                TextButton(onClick = {
                    // Stop monitoring the app
                    // We would call the ViewModel here
                    selectedApp = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedApp = null }) {
                    Text("No")
                }
            }
        )
    }

    // Screen to show unmonitored apps
    if (showUnmonitoredApps) {
        UnmonitoredAppsScreen(
            onDismiss = { showUnmonitoredApps = false },
            onAppSelected = { appName, packageName ->
                // Add app to monitored list
                // We would call the ViewModel here
                showUnmonitoredApps = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppItem(appName: String, packageName: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = packageName,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun UnmonitoredAppsScreen(
    onDismiss: () -> Unit,
    onAppSelected: (appName: String, packageName: String) -> Unit
) {
    // In a real app, we would get this from a ViewModel
    val unmonitoredApps = remember {
        listOf(
            "Chrome" to "com.android.chrome",
            "YouTube" to "com.google.android.youtube",
            "Twitter" to "com.twitter.android"
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select an app to monitor") },
        text = {
            LazyColumn {
                items(unmonitoredApps) { (appName, packageName) ->
                    TextButton(
                        onClick = { onAppSelected(appName, packageName) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(appName)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}