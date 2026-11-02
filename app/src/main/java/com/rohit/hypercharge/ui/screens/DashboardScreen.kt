package com.rohit.hypercharge.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.rohit.hypercharge.util.NotificationHelper
import com.rohit.hypercharge.util.RootShell
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen() {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var batteryStats by remember { mutableStateOf(BatteryStats()) }
    var currentMode by remember { mutableStateOf(ChargeMode.MODE_8A) }
    var isRootGranted by remember { mutableStateOf(true) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var updateStatus by remember { mutableStateOf<UpdateStatus>(UpdateStatus.Idle) }
    var showUpdateDialog by remember { mutableStateOf(false) }

    // Real-Time Battery & Hardware Polling Loop (1000ms Real-Time updates)
    LaunchedEffect(Unit) {
        isRootGranted = RootShell.isRootAvailable()
        currentMode = RootShell.getCurrentMode()
        RootShell.ensurePermissionsAndDaemon(context)

        while (true) {
            batteryStats = RootShell.readBatteryStats(context)
            if (batteryStats.isCharging) {
                NotificationHelper.showChargingNotification(context, batteryStats, currentMode)
            } else {
                NotificationHelper.dismissNotification(context)
            }
            delay(1000)
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
                            "Built by Rohit Sharma • Build 8 Controller",
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
                                "Root access required. Please grant root in KernelSU / Magisk to apply manual charging modes.",
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

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                if (batteryStats.wattageWatts > 0f) {
                                    Surface(
                                        color = CyanAccent.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Text(
                                            text = String.format(java.util.Locale.US, "%.1f W", batteryStats.wattageWatts),
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = CyanAccent,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                }
                                Text(
                                    text = "${batteryStats.percentage}%",
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = CyanAccent
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Real-Time High-Impact Amperage & Wattage Flow Card
                        Surface(
                            color = DarkSurfaceVariant,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "REAL-TIME FLOW",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = TextSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(verticalAlignment = Alignment.Bottom) {
                                        Text(
                                            text = if (batteryStats.currentMilliAmps > 0)
                                                String.format(java.util.Locale.US, "%.2f A", batteryStats.currentAmperes)
                                            else "-- A",
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (batteryStats.currentAmperes >= 7.8f) WarningOrange else CyanAccent
                                        )
                                        if (batteryStats.currentMilliAmps > 0) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(
                                                text = "${batteryStats.currentMilliAmps} mA",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = TextSecondary,
                                                modifier = Modifier.padding(bottom = 3.dp)
                                            )
                                        }
                                    }
                                }

                                if (batteryStats.wattageWatts > 0f) {
                                    Surface(
                                        color = (if (batteryStats.wattageWatts >= 28f) WarningOrange else CyanAccent).copy(alpha = 0.18f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Column(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            horizontalAlignment = Alignment.End
                                        ) {
                                            Text(
                                                text = String.format(java.util.Locale.US, "%.1f W", batteryStats.wattageWatts),
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (batteryStats.wattageWatts >= 28f) WarningOrange else CyanAccent
                                            )
                                            Text(
                                                text = "POWER FLOW",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (batteryStats.wattageWatts >= 28f) WarningOrange else CyanAccent
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Stats Grid (Temp, Amperage, Voltage)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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

                            StatPill(
                                label = "Amperage Flow",
                                value = if (batteryStats.currentMilliAmps > 0)
                                    String.format(java.util.Locale.US, "%.2f A", batteryStats.currentAmperes)
                                else "--",
                                icon = Icons.Default.ElectricBolt,
                                color = if (batteryStats.currentAmperes >= 7.8f) WarningOrange else CyanAccent
                            )

                            StatPill(
                                label = "Voltage",
                                value = if (batteryStats.voltageVolts > 0) String.format(java.util.Locale.US, "%.2f V", batteryStats.voltageVolts) else "--",
                                icon = Icons.Default.Speed,
                                color = ElectricBlue
                            )
                        }
                    }
                }
            }

            // Master Engine Status Card (Active vs Stock Revert)
            item {
                val isEngineDisabled = currentMode == ChargeMode.STOCK
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isEngineDisabled) DangerRed.copy(alpha = 0.18f) else DarkSurface
                    ),
                    border = BorderStroke(
                        1.5.dp,
                        if (isEngineDisabled) DangerRed else TextSecondary.copy(alpha = 0.2f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val newMode = if (isEngineDisabled) ChargeMode.MODE_8A else ChargeMode.STOCK
                            currentMode = newMode
                            coroutineScope.launch {
                                RootShell.applyMode(newMode)
                                val msg = if (newMode == ChargeMode.STOCK)
                                    "🛑 Engine Disabled! Reverted to Stock Control."
                                else
                                    "✔ 8 Ampere Turbo Engine Activated!"
                                Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background(
                                        if (isEngineDisabled) DangerRed.copy(alpha = 0.25f) else NeonGreen.copy(alpha = 0.15f)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (isEngineDisabled) Icons.Default.Block else Icons.Default.PowerSettingsNew,
                                    contentDescription = null,
                                    tint = if (isEngineDisabled) DangerRed else NeonGreen
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (isEngineDisabled) "Engine Status: STOCK (OFF)" else "Engine Status: ACTIVE",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isEngineDisabled) DangerRed else NeonGreen
                                )
                                Text(
                                    text = if (isEngineDisabled) "Stock charging in control • Tap to activate Turbo" else "Tap to turn OFF all overrides & revert to stock",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Switch(
                            checked = !isEngineDisabled,
                            onCheckedChange = { isChecked ->
                                val target = if (isChecked) ChargeMode.MODE_8A else ChargeMode.STOCK
                                currentMode = target
                                coroutineScope.launch {
                                    RootShell.applyMode(target)
                                    Toast.makeText(context, if (isChecked) "8A Engine Activated" else "Stock Revert Activated", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = DarkBg,
                                checkedTrackColor = NeonGreen,
                                uncheckedThumbColor = TextSecondary,
                                uncheckedTrackColor = DarkSurfaceVariant
                            )
                        )
                    }
                }
            }

            // Notification Center Indicator
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface),
                    border = BorderStroke(1.dp, if (batteryStats.isCharging) NeonGreen.copy(alpha = 0.35f) else TextSecondary.copy(alpha = 0.15f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Box(
                                modifier = Modifier
                                    .size(42.dp)
                                    .clip(CircleShape)
                                    .background((if (batteryStats.isCharging) NeonGreen else CyanAccent).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.NotificationsActive,
                                    contentDescription = null,
                                    tint = if (batteryStats.isCharging) NeonGreen else CyanAccent
                                )
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Notification Center: Dual-Engine (Build 8)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                Text(
                                    text = if (batteryStats.isCharging) "⚡ Live charging panel active in status bar" else "Automatically appears when charger is plugged in",
                                    fontSize = 11.sp,
                                    color = TextSecondary
                                )
                            }
                        }
                        Surface(
                            color = (if (batteryStats.isCharging) NeonGreen else TextSecondary).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (batteryStats.isCharging) "● LIVE AUTO" else "AUTO STANDBY",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (batteryStats.isCharging) NeonGreen else TextSecondary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }

            // Section Header: Dedicated Mode Selection
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Governor Mode Switch (Strict 6A - 8A Band)",
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

            // =========================================================================
            // BUTTON 1: 8 Ampere Turbo (30W - 32W) - Strict Upper Ceiling Cap (Never > 8A)
            // =========================================================================
            item {
                val isSelected = currentMode == ChargeMode.MODE_8A
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) WarningOrange else Color.Transparent,
                    label = "border8A"
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) WarningOrange.copy(alpha = 0.16f) else DarkSurface
                    ),
                    border = BorderStroke(2.dp, if (isSelected) WarningOrange else TextSecondary.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentMode = ChargeMode.MODE_8A
                            coroutineScope.launch {
                                val success = RootShell.applyMode(ChargeMode.MODE_8A)
                                if (success) {
                                    if (batteryStats.isCharging) {
                                        NotificationHelper.showChargingNotification(context, batteryStats, ChargeMode.MODE_8A)
                                    }
                                    Toast.makeText(context, "⚡ 8 Ampere Turbo Activated!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(WarningOrange.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.ElectricBolt, contentDescription = null, tint = WarningOrange, modifier = Modifier.size(26.dp))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "8 Ampere Turbo",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) WarningOrange else TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = WarningOrange.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "8.0A CONSTANT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = WarningOrange,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "30W - 32W High Speed • Strict Upper Ceiling (Never > 8A)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = WarningOrange,
                                modifier = Modifier.size(26.dp)
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // BUTTON 2: 6 Ampere Stable (22W - 25W) - Solid Floor (Strict 6A - 8A Band)
            // =========================================================================
            item {
                val isSelected = currentMode == ChargeMode.MODE_6A
                val borderColor by animateColorAsState(
                    targetValue = if (isSelected) CyanAccent else Color.Transparent,
                    label = "border6A"
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) CyanAccent.copy(alpha = 0.16f) else DarkSurface
                    ),
                    border = BorderStroke(2.dp, if (isSelected) CyanAccent else TextSecondary.copy(alpha = 0.15f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            currentMode = ChargeMode.MODE_6A
                            coroutineScope.launch {
                                val success = RootShell.applyMode(ChargeMode.MODE_6A)
                                if (success) {
                                    if (batteryStats.isCharging) {
                                        NotificationHelper.showChargingNotification(context, batteryStats, ChargeMode.MODE_6A)
                                    }
                                    Toast.makeText(context, "⚡ 6 Ampere Stable Activated!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(46.dp)
                                .clip(CircleShape)
                                .background(CyanAccent.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(26.dp))
                        }

                        Spacer(modifier = Modifier.width(14.dp))

                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "6 Ampere Stable",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) CyanAccent else TextPrimary
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Surface(
                                    color = CyanAccent.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        "6.0A CONSTANT",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = CyanAccent,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "22W - 25W Smooth Flow • Solid Floor (Strict 6A - 8A Band)",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active",
                                tint = CyanAccent,
                                modifier = Modifier.size(26.dp)
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
