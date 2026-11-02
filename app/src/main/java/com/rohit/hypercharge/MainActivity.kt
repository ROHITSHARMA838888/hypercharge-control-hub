package com.rohit.hypercharge

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.rohit.hypercharge.ui.screens.DashboardScreen
import com.rohit.hypercharge.ui.screens.SplashScreen
import com.rohit.hypercharge.ui.theme.DarkBg
import com.rohit.hypercharge.ui.theme.HyperChargeTheme
import com.rohit.hypercharge.util.RootShell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        // Permission result handled; RootShell also grants via root as fail-safe
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // 1. Request POST_NOTIFICATIONS runtime permission on Android 13+ (API 33+)
        requestNotificationPermissionIfNeeded()

        // 2. Proactively grant permissions and ensure daemon is running via root
        lifecycleScope.launch(Dispatchers.IO) {
            RootShell.ensurePermissionsAndDaemon(applicationContext)
        }

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

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}

