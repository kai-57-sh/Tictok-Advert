package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.data.AppContainer
import com.example.ui.TictokAdvertApp
import com.example.ui.theme.TictokAdvertTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Initialize App Database and Repository Container
        AppContainer.initialize(applicationContext)
        
        // Enable true full edge-to-edge transparent drawing
        enableEdgeToEdge()
        
        setContent {
            TictokAdvertTheme {
                TictokAdvertApp()
            }
        }
    }
}
