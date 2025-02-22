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
import com.example.cmiyc.ui.components.FriendItem
import com.example.cmiyc.ui.components.SearchBar
import com.example.cmiyc.ui.viewmodels.FriendsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FriendsScreen(
    onNavigateBack: () -> Unit,
    viewModel: FriendsViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Friends") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
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
                                onAddFriend = { viewModel.addFriend(friend) },
                                onRemoveFriend = { /* Not needed here */ }
                            )
                        }
                    } else {
                        items(state.friends) { friend ->
                            FriendItem(
                                friend = friend,
                                isFriend = true,
                                onAddFriend = { /* Not needed here */ },
                                onRemoveFriend = { viewModel.removeFriend(friend) }
                            )
                        }
                    }
                }
            }
        }

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