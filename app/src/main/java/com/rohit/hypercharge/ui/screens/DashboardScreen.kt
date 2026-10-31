package com.rohit.hypercharge.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.rohit.hypercharge.ui.theme.*
import com.rohit.hypercharge.updater.AppUpdater
import com.rohit.hypercharge.updater.UpdateStatus
import com.rohit.hypercharge.util.BatteryStats
import com.rohit.hypercharge.util.ChargeMode
import com.rohit.hypercharge.util.RootShell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var batteryStats by remember { mutableStateOf(BatteryStats()) }
    var currentMode by remember { mutableStateOf(ChargeMode.AUTO) }
    var isRootGranted by remember { mutableStateOf(true) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Live Polling Loop
    LaunchedEffect(Unit) {
        isRootGranted = RootShell.isRootAvailable()
        currentMode = RootShell.getCurrentMode()

        while (true) {
            batteryStats = RootShell.readBatteryStats()
            delay(2000)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            "HyperCharge Control Hub",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            "Enhanced by Rohit Sharma",
                            fontSize = 11.sp,
                            color = CyanAccent,
                            fontWeight = FontWeight.Medium
                        )
                    }
                },
                actions = {
                    // Check for Updates Button
                    IconButton(onClick = {
                        coroutineScope.launch {
                            updateStatus = UpdateStatus.Checking
                            val result = AppUpdater.checkForUpdates()
                            updateStatus = result
                            showUpdateDialog = true
                        }
                    }) {
                        Icon(
                            imageVector = Icons.Default.SystemUpdate,
                            contentDescription = "Check Updates",
                            tint = CyanAccent
                        )
                    }
                    // About Button
                    IconButton(onClick = { showAboutDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "About",
                            tint = TextSecondary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkBg)
            )
        },
        containerColor = DarkBg
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Root Warning if not available
            if (!isRootGranted) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = DangerRed.copy(alpha = 0.2f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Warning, contentDescription = null, tint = DangerRed)
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                "Root access not detected. Please grant root in KernelSU / Magisk to apply modes.",
                                fontSize = 12.sp,
                                color = TextPrimary
                            )
                        }
                    }
                }
            }

            // Real-Time Battery Monitor Card
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(if (batteryStats.isCharging) NeonGreen else TextSecondary)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = if (batteryStats.isCharging) "Charging Active" else "On Battery",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (batteryStats.isCharging) NeonGreen else TextSecondary
                                )
                            }
                            Text(
                                text = "${batteryStats.percentage}%",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyanAccent
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Stats Grid (Temp, Current, Voltage)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Temperature Pill
                            val tempColor = when {
                                batteryStats.temperatureCelsius < 36f -> NeonGreen
                                batteryStats.temperatureCelsius < 41f -> WarningOrange
                                else -> DangerRed
                            }
                            StatPill(
                                label = "Battery Temp",
                                value = "${batteryStats.temperatureCelsius}°C",
                                icon = Icons.Default.Thermostat,
                                color = tempColor
                            )

                            // Current Pill
                            StatPill(
                                label = "Current Flow",
                                value = if (batteryStats.currentMilliAmps > 0) "${batteryStats.currentMilliAmps} mA" else "--",
                                icon = Icons.Default.ElectricBolt,
                                color = CyanAccent
                            )

                            // Voltage Pill
                            StatPill(
                                label = "Voltage",
                                value = if (batteryStats.voltageVolts > 0) String.format("%.2f V", batteryStats.voltageVolts) else "--",
                                icon = Icons.Default.Speed,
                                color = ElectricBlue
                            )
                        }
                    }
                }
            }

            // Section Header: Mode Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Governor Mode Switch",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        "Active: ${currentMode.title}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = CyanAccent
                    )
                }
            }

            // List of Interactive Mode Cards
            items(ChargeMode.entries) { mode ->
                val isSelected = currentMode == mode
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) CyanAccent else Color.Transparent,
                    label = "border"
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) DarkSurfaceVariant else DarkSurface
                    ),
                    border = BorderStroke(1.5.dp, borderColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentMode = mode
                            coroutineScope.launch {
                                val success = RootShell.applyMode(mode)
                                if (success) {
                                    Toast.makeText(context, "${mode.title} Activated!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Mode Icon
                        val icon = when (mode) {
                            ChargeMode.TURBO -> Icons.Default.FlashOn
                            ChargeMode.BALANCED -> Icons.Default.Balance
                            ChargeMode.ECO -> Icons.Default.Eco
                            ChargeMode.BYPASS -> Icons.Default.Gamepad
                            ChargeMode.AUTO -> Icons.Default.Autorenew
                        }
                        val iconColor = when (mode) {
                            ChargeMode.TURBO -> WarningOrange
                            ChargeMode.BALANCED -> CyanAccent
                            ChargeMode.ECO -> NeonGreen
                            ChargeMode.BYPASS -> ElectricBlue
                            ChargeMode.AUTO -> CyanAccent
                        }

                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(iconColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = iconColor)
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = mode.title,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (isSelected) CyanAccent else TextPrimary
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = mode.subtitle,
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        // Selected Radio / Check
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = CyanAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // About Dialog
    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    // In-App Update Dialog
    if (showUpdateDialog) {
        AlertDialog(
            onDismissRequest = { showUpdateDialog = false },
            title = {
                Text(
                    text = when (updateStatus) {
                        is UpdateStatus.Checking -> "Checking for Updates..."
                        is UpdateStatus.Available -> "New Update Available! 🚀"
                        is UpdateStatus.UpToDate -> "You're Up to Date! ✔"
                        is UpdateStatus.Error -> "Update Check Failed"
                        else -> "Updates"
                    },
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            },
            text = {
                when (val status = updateStatus) {
                    is UpdateStatus.Checking -> {
                        Box(modifier = Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = CyanAccent)
                        }
                    }
                    is UpdateStatus.Available -> {
                        Column {
                            Text("Version: ${status.versionTag}", fontWeight = FontWeight.SemiBold, color = CyanAccent)
                            Spacer(modifier = Modifier.height(6.dp))
                            Text("Changelog:\n${status.releaseNotes}", fontSize = 12.sp, color = TextSecondary)
                        }
                    }
                    is UpdateStatus.UpToDate -> {
                        Text("You are using the latest version (${status.currentVersion}). No update required.", fontSize = 13.sp, color = TextSecondary)
                    }
                    is UpdateStatus.Error -> {
                        Text(status.message, fontSize = 13.sp, color = DangerRed)
                    }
                    else -> {}
                }
            },
            confirmButton = {
                if (updateStatus is UpdateStatus.Available) {
                    val status = updateStatus as UpdateStatus.Available
                    Button(
                        onClick = {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(status.downloadUrl))
                            context.startActivity(intent)
                            showUpdateDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent)
                    ) {
                        Text("Download & Install", color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                } else {
                    TextButton(onClick = { showUpdateDialog = false }) {
                        Text("Close", color = CyanAccent)
                    }
                }
            },
            containerColor = DarkSurface
        )
    }
}

@Composable
fun StatPill(label: String, value: String, icon: androidx.compose.ui.graphics.vector.ImageVector, color: Color) {
    Surface(
        color = DarkSurfaceVariant,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.width(100.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(label, fontSize = 9.sp, color = TextSecondary)
        }
    }
}
