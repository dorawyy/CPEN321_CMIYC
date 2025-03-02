package com.example.cmiyc.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.cmiyc.utils.GeocodingUtil
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.data.Log
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.ui.viewmodels.LogViewModel
import com.example.cmiyc.ui.viewmodels.LogViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel: LogViewModel = viewModel(
        factory = LogViewModelFactory(
            userRepository = UserRepository
        )
    )
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Log") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading && state.logs.isEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                if (state.logs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No activities yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val sortedLogs = state.logs.sortedByDescending { it.timestamp }
                        itemsIndexed(
                            items = sortedLogs,
                            key = { index, log -> "${index}_${log.sender}${log.timestamp}" }
                        ) { index, log ->
                            val logId = "${log.sender}${log.timestamp}"
                            val cachedAddress = state.logAddresses[logId]
                            LogItem(
                                log = log,
                                address = cachedAddress,
                                onAddressLoaded = { address ->
                                    viewModel.updateLogAddress(logId, address)
                                }
                            )
                        }
                    }
                }
            }

            // Show loading spinner when refreshing with existing logs
            if (state.isLoading && state.logs.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }
        }

        // Error Dialog (for one-time errors)
        state.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }

        // Persistent Refresh Error Dialog (after multiple failures)
        state.refreshError?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearRefreshError() },
                title = { Text("Sync Problem") },
                text = {
                    Column {
                        Text(error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("The app will continue to try refreshing in the background.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { viewModel.clearRefreshError() }) {
                        Text("OK")
                    }
                },
            )
        }
    }
}

@Composable
fun LogItem(
    log: Log,
    address: String? = null,
    onAddressLoaded: (String) -> Unit = {}
) {
    val context = LocalContext.current
    var locationText by remember { mutableStateOf(address ?: "Loading address...") }

    // Fetch address if not provided
    LaunchedEffect(log.senderLocation) {
        if (address == null) {
            locationText = "Loading address..."
            val result = GeocodingUtil.getAddressFromLocation(
                context,
                log.senderLocation.latitude(),
                log.senderLocation.longitude()
            )
            locationText = result
            onAddressLoaded(result)
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = log.activity,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "From: ${log.sender}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Location: $locationText",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Text(
                text = formatTimestamp(log.timestamp),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Double.format(digits: Int) = "%.${digits}f".format(this)

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}