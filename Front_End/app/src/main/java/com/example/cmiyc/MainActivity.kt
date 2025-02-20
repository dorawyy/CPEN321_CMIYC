package com.example.cmiyc

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat
import com.example.cmiyc.ui.theme.CMIYCTheme
import android.Manifest
import android.util.Log
import android.widget.Toast
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.firestore
import com.google.firebase.Firebase
import com.google.firebase.initialize
import com.google.firebase.messaging.messaging


class MainActivity : ComponentActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val NOTIFICATION_REQUEST_CODE = 1234
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Firebase.initialize(this)
        logRegToken()
        setContent {
            CMIYCTheme {
                LaunchedEffect(Unit) {
                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    finish() // Close MainActivity so user can't go back
                }
            }
        }
    }

    fun logRegToken() {
        Firebase.messaging.getToken().addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                return@addOnCompleteListener
            }
            val token = task.result
            val msg = "FCM Registration token: $token"
            Log.d(TAG, msg)
            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
        }
    }

}