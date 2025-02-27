package com.example.cmiyc

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.example.cmiyc.ui.theme.CMIYCTheme
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import com.google.firebase.Firebase
import com.google.firebase.messaging.messaging
import androidx.compose.material3.Surface
import com.example.cmiyc.navigation.AppNavigation
import com.google.firebase.initialize

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        Firebase.initialize(this)
        setContent {
            CMIYCTheme {
                Surface {
                    AppNavigation()
                }
            }
        }
    }
}