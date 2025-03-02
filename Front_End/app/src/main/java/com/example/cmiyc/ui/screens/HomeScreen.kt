import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.R
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.repository.FriendsRepository
import com.example.cmiyc.ui.components.MapComponent
import com.example.cmiyc.ui.viewmodels.HomeViewModel
import com.example.cmiyc.ui.viewmodels.HomeViewModelFactory
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToProfile: () -> Unit,
    onNavigateToLog: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToAdmin: () -> Unit,
) {
    val viewModel: HomeViewModel = viewModel(factory = HomeViewModelFactory())
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var userInput by remember { mutableStateOf("") }
    val state by viewModel.state.collectAsState()
    val mapViewportState = rememberMapViewportState()

    // Collect isAdmin state from repository
    val isAdmin by UserRepository.isAdmin.collectAsState()

    // Collect registration complete state
    val isRegistrationComplete by UserRepository.isRegistrationComplete.collectAsState()

    // Start polling when screen becomes active AND registration is complete, stop when inactive
    DisposableEffect(isRegistrationComplete) {
        // Only start polling if registration is complete
        if (isRegistrationComplete) {
            viewModel.startPolling()
        }

        onDispose {
            if (isRegistrationComplete) {
                viewModel.stopPolling()
            }
        }
    }

    val snackbarHostState = remember { SnackbarHostState() }

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

                    // Show admin button for admin users
                    if (isAdmin) {
                        IconButton(onClick = onNavigateToAdmin) {
                            Icon(
                                Icons.Default.AdminPanelSettings,
                                contentDescription = "Admin Panel"
                            )
                        }
                    }

                    IconButton(onClick = onNavigateToProfile) {
                        Icon(
                            painter = painterResource(id = R.drawable.user_profile_icon),
                            contentDescription = "Profile"
                        )
                    }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState)
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
                title = { Text("Activity Update") },
                text = {
                    TextField(
                        value = userInput,
                        onValueChange = { userInput = it },
                        label = { Text("Your Activity") }
                    )
                },
                confirmButton = {
                    Button(onClick = {
                        showDialog = false
                        viewModel.broadcastMessage(userInput)
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

        // Error handling for broadcast errors
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

        // Error handling for polling errors
        state.pollingError?.let { error ->
            AlertDialog(
                onDismissRequest = { viewModel.clearPollingError() },
                title = { Text("Connection Issue") },
                text = {
                    Column {
                        Text(error)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("The app will continue to try updating in the background.")
                    }
                },
                confirmButton = {
                    Button(onClick = { viewModel.clearPollingError() }) {
                        Text("OK")
                    }
                }
            )
        }

        LaunchedEffect(state.broadcastSuccess) {
            if (state.broadcastSuccess) {
                // Show success snackbar
                snackbarHostState.showSnackbar(
                    message = "Activity broadcast successfully!",
                    duration = SnackbarDuration.Short
                )
                viewModel.clearBroadcastSuccess()
            }
        }
    }
}