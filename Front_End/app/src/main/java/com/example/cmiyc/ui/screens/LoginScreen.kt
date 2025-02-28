package com.example.cmiyc.ui.screens

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current.applicationContext

    val viewModel: LoginViewModel = viewModel(
        factory = LoginViewModelFactory(
            userRepository = UserRepository,
        )
    )
    val loginState by viewModel.loginState.collectAsState()

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