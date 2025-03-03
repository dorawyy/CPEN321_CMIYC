package com.example.cmiyc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.data.AdminUserItem
import com.example.cmiyc.ui.components.SearchBar
import com.example.cmiyc.ui.viewmodels.AdminViewModel
import com.example.cmiyc.ui.viewmodels.AdminViewModelFactory

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
) {
    val viewModel: AdminViewModel = viewModel(factory = AdminViewModelFactory())
    val state by viewModel.state.collectAsState()

    // Track user being banned for confirmation dialog
    var userToBan by remember { mutableStateOf<AdminUserItem?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadUsers()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Admin Panel") },
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
                query = state.filterQuery,
                onQueryChange = viewModel::filterUsers,
                placeholder = "Search users",
                modifier = Modifier.padding(16.dp)
            )

            Box(modifier = Modifier.fillMaxSize()) {
                if (state.isLoading && state.filteredUsers.isEmpty()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else if (state.filteredUsers.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.filterQuery.isEmpty())
                                "No users found"
                            else
                                "No matching users",
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
                        itemsIndexed(
                            items = state.filteredUsers,
                            key = { index, user -> "${user.userId}_${index}" }
                        ) { index, user ->
                            AdminUserItem(
                                user = user,
                                onBanClick = { userToBan = user }
                            )
                        }
                    }
                }
            }
        }

        // Ban confirmation dialog
        userToBan?.let { user ->
            AlertDialog(
                onDismissRequest = { userToBan = null },
                title = { Text("Ban User") },
                text = {
                    Text("Are you sure you want to ban ${user.name} (${user.email})?")
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            viewModel.banUser(user.userId)
                            userToBan = null
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Ban User")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { userToBan = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Error dialog
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
fun AdminUserItem(
    user: AdminUserItem,
    onBanClick: () -> Unit,
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = user.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                // Show banned status if banned
                if (user.isBanned) {
                    Text(
                        text = "BANNED",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }

            // Only show ban button for non-banned users
            if (!user.isBanned) {
                IconButton(
                    onClick = onBanClick
                ) {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = "Ban User",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}