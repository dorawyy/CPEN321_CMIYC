package com.example.cmiyc.ui.screens

import android.app.Activity
import android.content.Intent
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.ui.viewmodels.LoginState
import com.example.cmiyc.ui.viewmodels.LoginViewModel
import com.example.cmiyc.ui.viewmodels.LoginViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import java.io.IOException

@Composable
fun LoginScreen(onLoginSuccess: (String, String, String) -> Unit) {
    val viewModel: LoginViewModel = viewModel(factory = LoginViewModelFactory(UserRepository))
    val loginState by viewModel.loginState.collectAsState()
    val activity = LocalContext.current as? Activity

    var showBannedDialog by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        try {
            val account = GoogleSignIn.getSignedInAccountFromIntent(result.data).getResult(ApiException::class.java)
            viewModel.handleSignInResult(account?.email, account?.displayName, account?.idToken, account?.photoUrl.toString())
        } catch (e: ApiException) {
            showErrorDialog = true
            errorMessage = if (e.statusCode == 7) "Network connection error." else "Failed to sign in."
        } catch (e: IOException) {
            showErrorDialog = true
            errorMessage = "Network error during sign in."
        }
    }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> onLoginSuccess(state.email, state.displayName, state.idToken)
            is LoginState.Banned -> showBannedDialog = true
            is LoginState.Error -> { showErrorDialog = true; errorMessage = state.message }
            else -> {}
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = "Catch Me If You Can", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(32.dp))
        GoogleSignInButton(viewModel, launcher)
    }

    if (showBannedDialog) BannedDialog { activity?.finish() }
    if (showErrorDialog) ErrorDialog(errorMessage) { showErrorDialog = false; viewModel.resetState() }
}


@Composable
fun BannedDialog(onExit: () -> Unit) {
    AlertDialog(
        onDismissRequest = { /* Don't dismiss on outside click */ },
        title = { Text("Account Banned") },
        text = { Text("Your account has been banned. Contact support for assistance.") },
        confirmButton = {
            TextButton(
                onClick = onExit
            ) {
                Text("Exit")
            }
        }
    )
}

@Composable
fun ErrorDialog(errorMessage: String, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Login Error") },
        text = { Text(errorMessage) },
        confirmButton = {
            TextButton(
                onClick = onDismiss
            ) {
                Text("OK")
            }
        }
    )
}

@Composable
fun GoogleSignInButton(viewModel: LoginViewModel, launcher: ManagedActivityResultLauncher<Intent, ActivityResult>) {
    val context = LocalContext.current
    var adminRequested by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = adminRequested,
            onCheckedChange = { adminRequested = it }
        )
        Text(
            text = "Test Admin",
            modifier = Modifier.padding(start = 8.dp)
        )
    }

    Button(
        onClick = {
            val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("154250715924-n0she152p8rq5gels8aak5d5g0t3ak9v.apps.googleusercontent.com")
                .requestEmail()
                .build()

            val signInClient = GoogleSignIn.getClient(context, gso)
            viewModel.setAdminRequested(adminRequested)
            launcher.launch(signInClient.signInIntent)
        },
        modifier = Modifier.fillMaxWidth().testTag("login_button"),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
    ) {
        Text(text = "Sign in with Google")
    }
}


