import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.R
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.repository.FriendsRepository
import com.example.cmiyc.ui.components.MapComponent
import com.example.cmiyc.ui.viewmodels.HomeScreenState
import com.example.cmiyc.ui.viewmodels.HomeViewModel
import com.example.cmiyc.ui.viewmodels.HomeViewModelFactory
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import kotlinx.coroutines.delay

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
    val isAdmin by UserRepository.isAdmin.collectAsState()
    val isRegistrationComplete by UserRepository.isRegistrationComplete.collectAsState()

    DisposableEffect(isRegistrationComplete) {
        if (isRegistrationComplete) viewModel.startPolling()
        onDispose { if (isRegistrationComplete) viewModel.stopPolling() }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        topBar = {
            TopBar(onNavigateToLog = onNavigateToLog, onNavigateToFriends = onNavigateToFriends, onNavigateToAdmin = onNavigateToAdmin, onNavigateToProfile = onNavigateToProfile, isAdmin = isAdmin)
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues), verticalArrangement = Arrangement.SpaceBetween, horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.weight(1f)) {
                MapComponent(context = context, mapViewportState = mapViewportState, friends = state.friends, modifier = Modifier.fillMaxSize())
            }

            Box(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)) {
                Button(
                    onClick = { showDialog = true },
                    modifier = Modifier.fillMaxWidth().padding(16.dp).testTag("broadcast_button")
                ) { Text("Broadcast") }
            }
        }

        BroadcastDialog(showDialog = showDialog, userInput = userInput, onUserInputChange = { userInput = it },
            onBroadcastConfirm = {
                showDialog = false
                viewModel.broadcastMessage(userInput)
                userInput = ""
            }, onDialogDismiss = { showDialog = false }
        )
        ErrorSnackbar(state = state, viewModel=viewModel, snackbarHostState = snackbarHostState)
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TopBar(
    onNavigateToLog: () -> Unit,
    onNavigateToFriends: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onNavigateToProfile: () -> Unit,
    isAdmin: Boolean
) {
    TopAppBar(
        title = {},
        navigationIcon = {
            IconButton(onClick = onNavigateToLog, modifier = Modifier.testTag("log_button")) {
                Icon(painter = painterResource(id = R.drawable.log_icon), contentDescription = "Log")
            }
        },
        actions = {
            IconButton(onClick = onNavigateToFriends, modifier = Modifier.testTag("friends_button")) {
                Icon(painter = painterResource(id = R.drawable.friends_svg), contentDescription = "Friends")
            }
            if (isAdmin) {
                IconButton(onClick = onNavigateToAdmin) {
                    Icon(Icons.Default.AdminPanelSettings, contentDescription = "Admin Panel")
                }
            }
            IconButton(onClick = onNavigateToProfile, modifier = Modifier.testTag("profile_button")) {
                Icon(painter = painterResource(id = R.drawable.user_profile_icon), contentDescription = "Profile")
            }
        }
    )
}


@Composable
fun BroadcastDialog(
    showDialog: Boolean,
    userInput: String,
    onUserInputChange: (String) -> Unit,
    onBroadcastConfirm: () -> Unit,
    onDialogDismiss: () -> Unit
) {
    if (showDialog) {
        AlertDialog(
            onDismissRequest = onDialogDismiss,
            title = { Text("Activity Update") },
            text = {
                TextField(
                    value = userInput,
                    onValueChange = onUserInputChange,
                    label = { Text("Your Activity") }
                )
            },
            confirmButton = {
                Button(onClick = onBroadcastConfirm) { Text("Update") }
            },
            dismissButton = {
                Button(onClick = onDialogDismiss) { Text("Cancel") }
            }
        )
    }
}


@Composable
fun ErrorSnackbar(state: HomeScreenState, viewModel: HomeViewModel, snackbarHostState: SnackbarHostState) {
    LaunchedEffect(state.broadcastSuccess) {
        if (state.broadcastSuccess) {
            snackbarHostState.showSnackbar(
                message = "Activity broadcast successfully!",
                duration = SnackbarDuration.Short
            )
        }
    }

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

}

