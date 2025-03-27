package com.example.cmiyc.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.ui.viewmodels.ProfileViewModel
import com.example.cmiyc.ui.viewmodels.ProfileViewModelFactory
import coil.compose.SubcomposeAsyncImage
import com.example.cmiyc.data.User

/**
 * Screen for displaying and managing user profile information.
 *
 * This screen shows the user's profile information including their profile picture,
 * display name, and email. It also provides a sign-out option. The screen handles
 * loading states, auth state changes, and error conditions with appropriate UI feedback.
 *
 * @param onNavigateBack Callback to navigate back to the previous screen.
 * @param onSignedOut Callback invoked when the user signs out.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit,
    onSignedOut: () -> Unit
) {
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModelFactory(UserRepository))
    val state by viewModel.state.collectAsState()

    LaunchedEffect(state.user) {
        if (state.user == null) onSignedOut()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profile") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else {
                state.user?.let { user ->
                    Column(
                        modifier = Modifier.fillMaxSize().padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        ProfilePicture(photoUrl = user.photoUrl)
                        Spacer(modifier = Modifier.height(24.dp))
                        ProfileInfo(user = user)
                        Spacer(modifier = Modifier.weight(1f))
                        SignOutButton(onSignOut = { viewModel.signOut() })
                    }
                }
            }
        }

        // Error Dialog
        state.error?.let { error ->
            AlertDialog(
                onDismissRequest = viewModel::clearError,
                title = { Text("Error") },
                text = { Text(error) },
                confirmButton = { TextButton(onClick = viewModel::clearError) { Text("OK") } }
            )
        }
    }
}

/**
 * Component that displays a labeled information row with an icon.
 *
 * This component is used to display profile information fields with a
 * consistent layout including an icon, label, and value.
 *
 * @param icon The vector icon to display before the information.
 * @param label The label text describing what the value represents.
 * @param value The actual information value to display.
 */
@Composable
fun ProfileInfoRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

/**
 * Button component for signing out of the application.
 *
 * This button uses error colors to indicate its destructive action and
 * includes an icon for visual clarity.
 *
 * @param onSignOut Callback invoked when the user clicks the sign-out button.
 */
@Composable
fun SignOutButton(onSignOut: () -> Unit) {
    Button(
        onClick = onSignOut,
        modifier = Modifier.fillMaxWidth().testTag("signout_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer
        )
    ) {
        Icon(Icons.Default.ExitToApp, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text("Sign Out")
    }
}

/**
 * Card component that displays user profile information.
 *
 * This component presents the user's personal information, such as
 * name and email, in a structured card layout with icons and labels.
 *
 * @param user The User object containing profile information to display.
 */
@Composable
fun ProfileInfo(user: User) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ProfileInfoRow(
                icon = Icons.Default.Person,
                label = "Name",
                value = user.displayName
            )

            Divider()

            ProfileInfoRow(
                icon = Icons.Default.Email,
                label = "Email",
                value = user.email
            )
        }
    }
}

/**
 * Component that displays the user's profile picture.
 *
 * This component asynchronously loads and displays the user's profile picture
 * from a URL. It handles loading states and errors by displaying appropriate
 * placeholders. The image is displayed in a circular shape with a border.
 *
 * @param photoUrl The URL of the user's profile photo, may be null.
 */
@Composable
fun ProfilePicture(photoUrl: String?) {
    SubcomposeAsyncImage(
        model = photoUrl,
        contentDescription = "Profile Picture",
        modifier = Modifier
            .size(120.dp)
            .clip(CircleShape)
            .border(
                width = 2.dp,
                color = MaterialTheme.colorScheme.primary,
                shape = CircleShape
            ),
        contentScale = ContentScale.Crop,
        error = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        loading = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(40.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    )
}
