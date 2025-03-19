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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.data.Log
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.ui.viewmodels.LogViewModel
import com.example.cmiyc.ui.viewmodels.LogViewModelFactory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Screen that displays activity logs.
 *
 * This screen shows a chronological list of activities from friends and system events.
 * Each log entry includes the activity description, sender information, location details,
 * and timestamp. The screen handles loading states, empty states, and error conditions
 * with appropriate UI feedback.
 *
 * @param onNavigateBack Callback to navigate back to the previous screen.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogScreen(onNavigateBack: () -> Unit) {
    val viewModel: LogViewModel = viewModel(factory = LogViewModelFactory(UserRepository))
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Activity Log") },
                navigationIcon = { IconButton(onClick = onNavigateBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") } }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            // Show loading spinner when refreshing with existing logs
            if (state.isLoading && state.logs.isNotEmpty()) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .padding(16.dp)
                )
            }

            if (state.logs.isEmpty() && !state.isLoading) {
                Text("No activities yet", modifier = Modifier.align(Alignment.Center), style = MaterialTheme.typography.bodyLarge)
            } else {
                LogList(state.logs, state.logAddresses, onAddressLoaded = { logId, address -> viewModel.updateLogAddress(logId, address) })
            }
            ErrorDialog(error = state.error, title = "Error", onDismiss = { viewModel.clearError() })
            ErrorDialog(error = state.refreshError, title = "Sync Problem", onDismiss = { viewModel.clearRefreshError() })
        }
    }
}

/**
 * Card component that displays a single log entry.
 *
 * This component shows detailed information about an activity log, including:
 * - The activity description
 * - The sender's name
 * - The location where the activity occurred (resolved to a human-readable address)
 * - The timestamp of the activity
 *
 * It handles asynchronous address resolution with appropriate loading states.
 *
 * @param log The Log object containing activity details.
 * @param address Optional pre-resolved address string. If null, the component will
 *               asynchronously resolve the address from coordinates.
 * @param onAddressLoaded Callback invoked when an address is resolved, allowing for caching.
 */
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

/**
 * Extension function to format a Double with specified number of decimal places.
 *
 * @param digits The number of decimal places to include.
 * @return A String representation of the Double with the specified precision.
 */
private fun Double.format(digits: Int) = "%.${digits}f".format(this)

/**
 * Formats a timestamp into a human-readable date and time string.
 *
 * Converts a Unix timestamp (milliseconds since epoch) to a formatted date
 * string in the pattern "MMM dd, yyyy HH:mm" (e.g., "Jan 15, 2023 14:30").
 *
 * @param timestamp The timestamp in milliseconds to format.
 * @return A human-readable date and time string.
 */
private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val format = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return format.format(date)
}

/**
 * Component that displays a scrollable list of log entries.
 *
 * This component renders a list of log entries sorted by timestamp (newest first),
 * with appropriate spacing and padding. It passes resolved addresses to individual
 * log items to prevent redundant geocoding operations.
 *
 * @param logs List of Log objects to display.
 * @param logAddresses Map of log IDs to resolved address strings for caching.
 * @param onAddressLoaded Callback invoked when an address is resolved, for updating the cache.
 */
@Composable
fun LogList(logs: List<Log>, logAddresses: Map<String, String>, onAddressLoaded: (String, String) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        itemsIndexed(logs.sortedByDescending { it.timestamp }) { _, log ->
            val logId = "${log.sender}${log.timestamp}"
            LogItem(log = log, address = logAddresses[logId], onAddressLoaded = { onAddressLoaded(logId, it) })
        }
    }
}

/**
 * Dialog component for displaying error messages.
 *
 * This reusable dialog shows error messages with a title and dismiss button.
 * It's conditionally displayed only when an error is present.
 *
 * @param error The error message to display, or null if no error is present.
 * @param title The title for the error dialog.
 * @param onDismiss Callback invoked when the dialog is dismissed.
 */
@Composable
fun ErrorDialog(error: String?, title: String, onDismiss: () -> Unit) {
    error?.let {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = { Text(title) },
            text = { Text(it) },
            confirmButton = { TextButton(onClick = onDismiss) { Text("OK") } }
        )
    }
}


