package com.example.cmiyc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.ui.components.FriendItem
import com.example.cmiyc.ui.components.SearchBar
import com.example.cmiyc.ui.viewmodels.FriendsScreenState
import com.example.cmiyc.ui.viewmodels.FriendsViewModel
import com.example.cmiyc.ui.viewmodels.FriendsViewModelFactory
import com.mapbox.maps.extension.style.expressions.dsl.generated.mod
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel: FriendsViewModel = viewModel(factory = FriendsViewModelFactory())
    val state by viewModel.state.collectAsState()

    // Track if we're doing a manual refresh (pull-to-refresh) vs initial load
    var isManualRefresh by remember { mutableStateOf(false) }

    // Track success messages
    var showSuccessSnackbar by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }

    // Call onScreenEnter once when the screen is entered
    LaunchedEffect(Unit) {
        viewModel.onScreenEnter()
    }

    DisposableEffect(Unit) {
        onDispose {
            viewModel.onScreenExit()
        }
    }

    // Setup pull-to-refresh
    val pullRefreshState = rememberPullRefreshState(
        refreshing = state.isLoading && isManualRefresh,
        onRefresh = {
            isManualRefresh = true
            viewModel.refresh()
        }
    )

    Scaffold(
        topBar = { FriendsTopBar(onNavigateBack, viewModel, state) },
        snackbarHost = { SnackbarHandler(showSuccessSnackbar, successMessage, onClick = {showSuccessSnackbar = false}) }
    ) { padding ->
        FriendsList(FriendsListParams(
            padding = padding,
            pullRefreshState = pullRefreshState,
            state = state,
            isManualRefresh = isManualRefresh
        ), viewModel) { name ->
            successMessage = "Removed ${name} from your friends"
            showSuccessSnackbar = true
        }

        // Reset isManualRefresh when loading completes
        LaunchedEffect(state.isLoading) {
            if (!state.isLoading && isManualRefresh) {
                isManualRefresh = false
            }
        }

        // Auto-hide success snackbar after a delay
        LaunchedEffect(showSuccessSnackbar) {
            if (showSuccessSnackbar) {
                delay(3000) // 3 seconds
                showSuccessSnackbar = false
            }
        }

        FriendsDialogs(state, viewModel) { requestId ->
            val requestName = state.friendRequests.find { it.userId == requestId }?.displayName ?: "User"
            viewModel.denyRequest(requestId)
            successMessage = "Declined friend request from $requestName"
            showSuccessSnackbar = true
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsTopBar(
    onNavigateBack: () -> Unit,
    viewModel: FriendsViewModel,
    state: FriendsScreenState
) {
    TopAppBar(
        title = { Text("Friends") },
        navigationIcon = {
            IconButton(onClick = onNavigateBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Back")
            }
        },
        actions = {
            IconButton(onClick = { viewModel.updateState { it.copy(showAddFriendDialog = true) } }, modifier = Modifier.testTag("addFriends_button") ) {
                Icon(Icons.Default.Add, contentDescription = "Add Friend")
            }
            BadgedBox(
                badge = { if (state.friendRequests.isNotEmpty()) Badge { Text(state.friendRequests.size.toString()) } }
            ) {
                IconButton(onClick = { viewModel.loadFriendRequests() }, modifier = Modifier.testTag("friendRequests_button")) {
                    Icon(Icons.Default.Email, contentDescription = "Friend Requests")
                }
            }
        }
    )
}

@Composable
fun SnackbarHandler(showSuccessSnackbar: Boolean, successMessage: String, onClick: () -> Unit) {
    if (showSuccessSnackbar) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            action = {
                TextButton(onClick = onClick) {
                    Text("Dismiss")
                }
            },
            dismissAction = {
                IconButton(onClick = onClick) {
                    Icon(Icons.Filled.Close, contentDescription = "Dismiss")
                }
            }
        ) {
            Text(successMessage)
        }
    }
}

@Composable
fun FriendsDialogs(state: FriendsScreenState, viewModel: FriendsViewModel, onDeny: (String) -> Unit) {
    // Add Friend Dialog
    if (state.showAddFriendDialog) {
        AddFriendDialog(
            email = state.emailInput,
            onEmailChange = viewModel::updateEmailInput,
            onSendRequest = {
                viewModel.sendFriendRequest()
                // Success message will be shown in the error dialog handling below if successful
            },
            onDismiss = {
                viewModel.updateState { it.copy(
                    showAddFriendDialog = false,
                    emailInput = ""
                )}
            }
        )
    }

    // Friend Requests Dialog with loading state
    if (state.showRequestsDialog) {
        FriendRequestDialog(
            requests = state.friendRequests,
            isLoading = state.isRequestsLoading,
            onAccept = { requestId ->
                // Find the friend request to get their name
                val requestName = state.friendRequests.find { it.userId == requestId }?.displayName ?: "User"
                viewModel.acceptRequest(requestId)
            },
            onDeny = onDeny,
            onDismiss = {
                viewModel.updateState { it.copy(showRequestsDialog = false) }
            }
        )
    }

    // Error Dialog
    state.error?.let { error ->
        AlertDialog(
            onDismissRequest = viewModel::clearMessages,
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                TextButton(onClick = viewModel::clearMessages) {
                    Text("OK")
                }
            }
        )
    }
}

data class FriendsListParams @OptIn(ExperimentalMaterialApi::class) constructor(
    val padding: PaddingValues,
    val pullRefreshState: PullRefreshState,
    val state: FriendsScreenState,
    val isManualRefresh: Boolean
)

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun FriendsList(params: FriendsListParams, viewModel: FriendsViewModel, onRemoveFriend: (String) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(params.padding)
            .pullRefresh(params.pullRefreshState)
    ) {
        Column {
            SearchBar(query = params.state.filterQuery, onQueryChange = viewModel::filterFriends, modifier = Modifier.padding(16.dp))

            Box(modifier = Modifier.fillMaxSize()) {
                if (params.state.isLoading && !params.isManualRefresh && params.state.filteredFriends.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else if (params.state.filteredFriends.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = if (params.state.filterQuery.isEmpty()) "No friends yet" else "No matching friends",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                } else {
                    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        itemsIndexed(
                            items = params.state.filteredFriends,
                            key = { index, friend -> "${friend.userId}_${index}" }
                        ) { _, friend ->
                            FriendItem(
                                friend = friend,
                                onRemoveFriend = {
                                    viewModel.removeFriend(friend.userId, onSuccess = { onRemoveFriend(friend.name) })
                                }
                            )
                        }
                    }
                }
            }
        }

        PullRefreshIndicator(
            refreshing = params.state.isLoading && params.isManualRefresh,
            state = params.pullRefreshState,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}

@Composable
fun AddFriendDialog(
    email: String,
    onEmailChange: (String) -> Unit,
    onSendRequest: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Friend") },
        text = {
            OutlinedTextField(
                value = email,
                onValueChange = onEmailChange,
                label = { Text("Friend's Email") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Email,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = { onSendRequest() }
                ),
                modifier = Modifier.fillMaxWidth().testTag("friendEmail_Input")
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSendRequest,
                enabled = email.isNotBlank(),
                modifier = Modifier.testTag("submitFriendEmail_button")
            ) {
                Text("Send Request")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun FriendRequestDialog(
    requests: List<FriendRequest>,
    isLoading: Boolean,
    onAccept: (String) -> Unit,
    onDeny: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Friend Requests",
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (requests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "No pending friend requests",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.heightIn(max = 300.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    itemsIndexed(
                        items = requests,
                        key = { index, request -> "${request.userId}_${index}" }
                    ) { index, request ->
                        FriendRequestItem(
                            request = request,
                            onAccept = onAccept,
                            onDeny = onDeny
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun FriendRequestItem(
    request: FriendRequest,
    onAccept: (String) -> Unit,
    onDeny: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = request.displayName,
                    style = MaterialTheme.typography.bodyLarge
                )
                Text(
                    text = formatTimestamp(request.timestamp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilledTonalIconButton(
                    onClick = { onAccept(request.userId) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.testTag("acceptFriend_button")
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Accept",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                FilledTonalIconButton(
                    onClick = { onDeny(request.userId) },
                    colors = IconButtonDefaults.filledTonalIconButtonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer
                    )
                ) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Deny",
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                }
            }
        }
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val date = Date(timestamp)
    val now = Date()
    val diff = now.time - date.time

    return when {
        diff < 1000 * 60 -> "Just now"
        diff < 1000 * 60 * 60 -> "${diff / (1000 * 60)} minutes ago"
        diff < 1000 * 60 * 60 * 24 -> "${diff / (1000 * 60 * 60)} hours ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }
}