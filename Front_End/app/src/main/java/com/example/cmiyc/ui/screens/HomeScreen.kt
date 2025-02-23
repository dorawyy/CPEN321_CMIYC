package com.example.cmiyc.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.R
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.repository.FriendsRepository
import com.example.cmiyc.ui.components.MapComponent
import com.example.cmiyc.ui.viewmodels.HomeViewModel
import com.example.cmiyc.ui.viewmodels.HomeViewModelFactory
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToFriends: () -> Unit,
) {
    val friendsRepository = remember { FriendsRepository(UserRepository) }

    val viewModel: HomeViewModel = viewModel(
        factory = HomeViewModelFactory(
            userRepository = UserRepository,
            friendsRepository = friendsRepository
        )
    )
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    val mapViewportState = rememberMapViewportState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onNavigateToLog) {
                        Icon(
                            painter = painterResource(id = R.drawable.log_icon),
                            contentDescription = "Log"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToFriends) {
                        Icon(
                            painter = painterResource(id = R.drawable.friends_svg),
                            contentDescription = "Friends"
                        )
                    }
                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            painter = painterResource(id = R.drawable.user_profile_icon),
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(modifier = Modifier.weight(1f)) {
                MapComponent(
                    context = context,
                    mapViewportState = mapViewportState,
                    friends = state.friends,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Broadcast")
                }
            }
        }

        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Status Update") },
                text = {
                    TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = { Text("Your Status") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showDialog = false
                        viewModel.updateStatus(userInput)
                        userInput = ""
                    }) {
                        Text("Update")
                    }
                },
                dismissButton = {
                    Button(onClick = { showDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Error handling
        state.error?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearError() },
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = {
                    Button(onClick = { viewModel.clearError() }) {
                        Text("OK")
                    }
                }
            )
        }
    }
}