package com.example.cmiyc

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import com.example.cmiyc.ui.theme.CMIYCTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            CMIYCTheme {
                LaunchedEffect(Unit) {
                    startActivity(Intent(this@MainActivity, HomeActivity::class.java))
                    finish() // Close MainActivity so user can't go back
                }
            }
        }
    }
}