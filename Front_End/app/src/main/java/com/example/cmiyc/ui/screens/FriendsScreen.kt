package com.example.cmiyc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.data.FriendRequest
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.repository.FriendsRepository
import com.example.cmiyc.ui.components.FriendItem
import com.example.cmiyc.ui.components.SearchBar
import com.example.cmiyc.ui.viewmodels.FriendsViewModel
import com.example.cmiyc.ui.viewmodels.FriendsViewModelFactory
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
) {
    val friendsRepository = remember { FriendsRepository(UserRepository) }
    val viewModel: FriendsViewModel = viewModel(
        factory = FriendsViewModelFactory(
            userRepository = UserRepository,
            friendsRepository = friendsRepository
        )
    )

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Add Friend Button
                    IconButton(
                        onClick = {
                            viewModel.updateState { it.copy(showAddFriendDialog = true) }
                        }
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Add Friend"
                        )
                    }

                    // Friend Requests Button with Badge
                    BadgedBox(
                        badge = {
                            if (state.friendRequests.isNotEmpty()) {
                                Badge { Text(state.friendRequests.size.toString()) }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = {
                                viewModel.updateState { it.copy(showRequestsDialog = true) }
                            }
                        ) {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = "Friend Requests"
                            )
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            Column {
                SearchBar(
                    query = state.filterQuery,
                    onQueryChange = viewModel::filterFriends,
                    modifier = Modifier.padding(16.dp)
                )

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (state.filteredFriends.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.filterQuery.isEmpty())
                                "No friends yet"
                            else
                                "No matching friends",
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
                        items(
                            items = state.filteredFriends,
                            key = { friend -> friend.userId }
                        ) { friend ->
                            FriendItem(
                                friend = friend,
                                onRemoveFriend = { viewModel.removeFriend(friend.userId)},
                            )
                        }
                    }
                }
            }
        }

        // Add Friend Dialog
        if (state.showAddFriendDialog) {
            AddFriendDialog(
                email = state.emailInput,
                onEmailChange = viewModel::updateEmailInput,
                onSendRequest = {
                    viewModel.sendFriendRequest()
                },
                onDismiss = {
                    viewModel.updateState { it.copy(
                        showAddFriendDialog = false,
                        emailInput = ""
                    )}
                }
            )
        }

        // Friend Requests Dialog
        if (state.showRequestsDialog) {
            FriendRequestDialog(
                requests = state.friendRequests,
                onAccept = { requestId ->
                    viewModel.acceptRequest(requestId)
                },
                onDeny = { requestId ->
                    viewModel.denyRequest(requestId)
                },
                onDismiss = {
                    viewModel.updateState { it.copy(showRequestsDialog = false) }
                }
            )
        }

        // Error Dialog
        state.error?.let { error ->
            AlertDialog(
                onDismissRequest = viewModel::clearError,
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    TextButton(onClick = viewModel::clearError) {
                        Text("OK")
                    }
                }
            )
        }
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
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            TextButton(
                onClick = onSendRequest,
                enabled = email.isNotBlank()
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
            if (requests.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    contentAlignment = Alignment.Center
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
                    items(
                        items = requests,
                        key = { request -> request.userId }
                    ) { request ->
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
            TextButton(onClick = onDismiss) {
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
                    )
                ) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = "Accept",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
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