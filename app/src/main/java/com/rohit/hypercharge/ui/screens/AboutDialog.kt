package com.rohit.hypercharge.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.rohit.hypercharge.ui.theme.*

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current

    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = DarkSurface),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "About & Credits",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close", tint = TextSecondary)
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Section 1: App Developer
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Person, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Application & Rebuild Developer", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = CyanAccent)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Rohit Sharma", fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(
                            "Designed and programmed the HyperCharge Control Hub companion app and enhanced the KernelSU multi-mode fast charging governor.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/ROHITSHARMA838888/hypercharge-control-hub"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = CyanAccent.copy(alpha = 0.15f))
                        ) {
                            Text("View App on GitHub", fontSize = 12.sp, color = CyanAccent)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 2: Base Module Credit
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Code, contentDescription = null, tint = ElectricBlue, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Original KernelSU Base Credit", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = ElectricBlue)
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Demonx4y (Hyper-Charging-Plus)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = TextPrimary)
                        Text(
                            "Original open-source thermal governor and sysfs management base. Rebuilt and enhanced by Rohit Sharma to add manual app hooks and universal safe-mode checks.",
                            fontSize = 12.sp,
                            color = TextSecondary,
                            lineHeight = 16.sp
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        FilledTonalButton(
                            onClick = {
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Demonx4y/Hyper-Charging-Plus"))
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.filledTonalButtonColors(containerColor = ElectricBlue.copy(alpha = 0.15f))
                        ) {
                            Text("Demonx4y Base GitHub", fontSize = 12.sp, color = ElectricBlue)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Section 3: Hardware Support Matrix
                Surface(
                    color = DarkSurfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Devices, contentDescription = null, tint = NeonGreen, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Device Compatibility Matrix", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = NeonGreen)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🟢 Xiaomi Snapdragon: 100% Working", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = NeonGreen)
                        Text("Optimized for POCO F-series (F3 to F7) and high-speed fast charging Qualcomm devices on AOSP.", fontSize = 11.sp, color = TextSecondary)

                        Spacer(modifier = Modifier.height(6.dp))
                        Text("🟡 Xiaomi MediaTek: Partial Support", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = WarningOrange)
                        Text("Basic thermal ladder works. Features safe node detection—if a node is missing, it skips it cleanly without crashing.", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }
    }
}
