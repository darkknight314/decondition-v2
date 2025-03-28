@file:OptIn(ExperimentalMaterial3Api::class)

package com.decondition.ui.screens.domains

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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController

@Composable
fun MonitoredDomainsScreen(navController: NavController) {
    // In a real app, we would get this from a ViewModel
    val sampleDomains = remember {
        listOf(
            "instagram.com",
            "facebook.com",
            "tiktok.com"
        )
    }

    var showAddDomainDialog by remember { mutableStateOf(false) }
    var selectedDomain by remember { mutableStateOf<String?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (sampleDomains.isEmpty()) {
            // Empty state
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "No domains being monitored",
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    "Tap + to add domains you want to monitor",
                    modifier = Modifier.padding(top = 8.dp),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            // List of monitored domains
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sampleDomains) { domain ->
                    DomainItem(domain) {
                        selectedDomain = domain
                    }
                }
            }
        }

        // Floating action button to add new domains
        FloatingActionButton(
            onClick = { showAddDomainDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
        }
    }

    // Dialog to confirm removing domain from monitoring
    if (selectedDomain != null) {
        AlertDialog(
            onDismissRequest = { selectedDomain = null },
            title = { Text("Stop Monitoring?") },
            text = { Text("Do you want to stop monitoring $selectedDomain?") },
            confirmButton = {
                TextButton(onClick = {
                    // Stop monitoring the domain
                    // We would call the ViewModel here
                    selectedDomain = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedDomain = null }) {
                    Text("No")
                }
            }
        )
    }

    // Dialog to add a new domain
    if (showAddDomainDialog) {
        AddDomainDialog(
            onDismiss = { showAddDomainDialog = false },
            onDomainAdded = { domain ->
                // Add domain to monitored list
                // We would call the ViewModel here
                showAddDomainDialog = false
            }
        )
    }
}

@Composable
fun DomainItem(domain: String, onClick: () -> Unit) {
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
                text = domain,
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
fun AddDomainDialog(
    onDismiss: () -> Unit,
    onDomainAdded: (String) -> Unit
) {
    var domain by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Domain") },
        text = {
            Column {
                Text(
                    "Enter a domain you want to monitor",
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                OutlinedTextField(
                    value = domain,
                    onValueChange = { domain = it },
                    label = { Text("Domain") },
                    placeholder = { Text("example.com") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (domain.isNotBlank()) {
                        onDomainAdded(domain.trim())
                    }
                }
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}