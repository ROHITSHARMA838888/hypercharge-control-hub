package com.rohit.hypercharge

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.rohit.hypercharge.ui.screens.DashboardScreen
import com.rohit.hypercharge.ui.screens.SplashScreen
import com.rohit.hypercharge.ui.theme.DarkBg
import com.rohit.hypercharge.ui.theme.HyperChargeTheme

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            HyperChargeTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBg
                ) {
                    var isSplashVisible by remember { mutableStateOf(true) }

                    Crossfade(targetState = isSplashVisible, label = "splash_fade") { showSplash ->
                        if (showSplash) {
                            SplashScreen(onSplashFinished = { isSplashVisible = false })
                        } else {
                            DashboardScreen()
                        }
                    }
                }
            }
        }
    }
}
