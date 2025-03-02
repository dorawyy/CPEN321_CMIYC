package com.example.cmiyc.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.cmiyc.repositories.UserRepository
import com.example.cmiyc.ui.theme.CMIYCTheme
import com.example.cmiyc.ui.viewmodels.LoginState
import com.example.cmiyc.ui.viewmodels.LoginViewModel
import com.example.cmiyc.ui.viewmodels.LoginViewModelFactory
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

@Composable
fun LoginScreen(
    onLoginSuccess: (String, String, String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity

    val viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(
            userRepository = UserRepository,
        )
    )
    val loginState by viewModel.loginState.collectAsState()

    // Track if banned dialog is showing
    var showBannedDialog by remember { mutableStateOf(false) }
    // Track if error dialog is showing
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            viewModel.handleSignInResult(
                account?.email,
                account?.displayName,
                account?.idToken,
                account?.photoUrl.toString(),
            )
        } catch (e: ApiException) {
            viewModel.handleSignInResult(null, null, null, null)
        }
    }

    LaunchedEffect(loginState) {
        when (val state = loginState) {
            is LoginState.Success -> {
                onLoginSuccess(state.email, state.displayName, state.idToken)
            }
            is LoginState.Banned -> {
                showBannedDialog = true
            }
            is LoginState.Error -> {
                errorMessage = state.message
                showErrorDialog = true
            }
            else -> {}
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Catch Me If You Can",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (loginState is LoginState.Loading) {
            // Show loading indicator
            CircularProgressIndicator(
                modifier = Modifier.padding(16.dp),
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "Signing in...",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )
        } else {
            Button(
                onClick = {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken("154250715924-n0she152p8rq5gels8aak5d5g0t3ak9v.apps.googleusercontent.com")
                        .requestEmail()
                        .build()

                    val signInClient = GoogleSignIn.getClient(context, gso)
                    launcher.launch(signInClient.signInIntent)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(text = "Sign in with Google")
            }
        }
    }

    // Show banned user dialog
    if (showBannedDialog) {
        AlertDialog(
            onDismissRequest = { /* Don't dismiss on outside click */ },
            title = { Text("Account Banned") },
            text = { Text("Your account has been banned. Contact support for assistance.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        activity?.finish() // Exit the app
                    }
                ) {
                    Text("Exit")
                }
            }
        )
    }

    // Show error dialog
    if (showErrorDialog) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = { Text("Login Error") },
            text = { Text(errorMessage) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showErrorDialog = false
                        viewModel.resetState() // Reset to initial state
                    }
                ) {
                    Text("OK")
                }
            }
        )
    }
}