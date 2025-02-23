package com.example.cmiyc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.repository.FriendsRepository
import com.example.cmiyc.ui.components.FriendItem
import com.example.cmiyc.ui.components.SearchBar
import com.example.cmiyc.ui.viewmodels.FriendsViewModel
import com.example.cmiyc.ui.viewmodels.FriendsViewModelFactory
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.ui.Alignment
import com.example.cmiyc.data.FriendRequest
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
                    // Friend Requests Button with Badge
                    BadgedBox(
                        badge = {
                            if (state.friendRequests.isNotEmpty()) {
                                Badge {
                                    Text(state.friendRequests.size.toString())
                                }
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
                    query = state.searchQuery,
                    onQueryChange = viewModel::searchFriends,
                    modifier = Modifier.padding(16.dp)
                )

                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (state.isSearching) {
                            items(state.searchResults) { friend ->
                                FriendItem(
                                    friend = friend,
                                    isFriend = false,
                                    onAddFriend = { viewModel.addFriend(friend.userId) },
                                    onRemoveFriend = { /* Not needed here */ }
                                )
                            }
                        } else {
                            items(state.friends) { friend ->
                                FriendItem(
                                    friend = friend,
                                    isFriend = true,
                                    onAddFriend = { /* Not needed here */ },
                                    onRemoveFriend = { viewModel.removeFriend(friend.userId) }
                                )
                            }
                        }
                    }
                }
            }
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
                    items(requests) { request ->
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
                    onClick = { onAccept(request.requestId) },
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
                    onClick = { onDeny(request.requestId) },
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