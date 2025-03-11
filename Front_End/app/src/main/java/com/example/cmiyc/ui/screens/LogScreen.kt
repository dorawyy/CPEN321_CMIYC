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


