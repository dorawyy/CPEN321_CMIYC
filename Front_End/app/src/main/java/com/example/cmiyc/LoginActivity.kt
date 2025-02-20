package com.example.cmiyc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.cmiyc.ui.theme.CMIYCTheme
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException

class LoginActivity : ComponentActivity() {
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            CMIYCTheme { // Apply your custom theme
                GoogleAuthScreen { startGoogleSignIn() }
            }
        }
    }

    private fun startGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("154250715924-n0she152p8rq5gels8aak5d5g0t3ak9v.apps.googleusercontent.com") // Replace with your Web Client ID
            .requestEmail()
            .build()

        val signInClient = GoogleSignIn.getClient(this, gso)
        startActivityForResult(signInClient.signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                // Handle the signed-in account
                val idToken = account?.idToken
                val email = account?.email
                val displayName = account?.displayName

                println("ID Token: $idToken")
                println("Email: $email")
                println("Display Name: $displayName")

                val intent = Intent(this, HomeActivity::class.java)
                intent.putExtra("email", email)
                intent.putExtra("displayName", displayName)
                intent.putExtra("idToken", idToken)

                startActivity(intent)
                finish()
                // Navigate to the next screen or perform other actions
            } catch (e: ApiException) {
                // Handle error
                println("Google sign-in failed: ${e.statusCode}")
            }
        }
    }
}

@Composable
fun GoogleAuthScreen(onSignInClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Catch Me If You Can",
            style = MaterialTheme.typography.headlineMedium, // Uses your theme's typography
            color = MaterialTheme.colorScheme.primary // Uses your theme's primary color
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onSignInClick,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary, // Uses your theme's primary color
                contentColor = MaterialTheme.colorScheme.onPrimary // Uses your theme's onPrimary color
            )
        ) {
            Text(text = "Sign in with Google")
        }
    }
}