package com.rohit.hypercharge.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohit.hypercharge.ui.theme.*
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(onSplashFinished: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        delay(2400)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(DarkBg, DarkSurface, DarkBg)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(24.dp)
        ) {
            // Glowing Animated Bolt Icon
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(CyanAccent.copy(alpha = 0.35f), ElectricBlue.copy(alpha = 0.15f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Bolt,
                    contentDescription = "Charge Icon",
                    tint = CyanAccent,
                    modifier = Modifier.size(64.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            // App Title
            Text(
                text = "HyperCharge Control Hub",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Subtitle with Rohit Sharma Credit
            Text(
                text = "⚡ Enhanced & Built by Rohit Sharma",
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = CyanAccent,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Module Purpose Pill
            Surface(
                color = DarkSurfaceVariant,
                shape = RoundedCornerShape(20.dp),
                border = ButtonDefaults.outlinedButtonBorder
            ) {
                Text(
                    text = "Built for Control: Hyper Charging+ [Rohit Rebuild]",
                    fontSize = 11.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        // Bottom Attribution Footer
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "App Developed by Rohit Sharma",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = TextPrimary.copy(alpha = 0.85f)
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "KernelSU Module Base: Demonx4y",
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
