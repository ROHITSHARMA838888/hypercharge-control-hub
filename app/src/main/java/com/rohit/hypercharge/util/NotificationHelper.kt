package com.rohit.hypercharge.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.rohit.hypercharge.MainActivity
import java.util.Locale

object NotificationHelper {

    private const val CHANNEL_ID = "hypercharge_live_monitor"
    private const val CHANNEL_NAME = "HyperCharge Live Charging Monitor"
    private const val NOTIFICATION_ID = 8801

    private var chargeStartTimeMillis: Long = 0L
    private var animFrame = 0

    fun showChargingNotification(context: Context, stats: BatteryStats, mode: ChargeMode) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Create notification channel if needed
        createChannel(context, notificationManager)

        if (chargeStartTimeMillis == 0L) {
            chargeStartTimeMillis = System.currentTimeMillis()
        }

        val elapsedSeconds = (System.currentTimeMillis() - chargeStartTimeMillis) / 1000L
        val hours = elapsedSeconds / 3600
        val mins = (elapsedSeconds % 3600) / 60
        val secs = elapsedSeconds % 60
        val timeDisplay = if (hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)
        } else {
            String.format(Locale.US, "%02d:%02d", mins, secs)
        }

        val amps = if (stats.currentMilliAmps > 0) stats.currentAmperes else 0f
        val watts = stats.wattageWatts
        val ampFormatted = String.format(Locale.US, "%.2f A", amps)
        val wattFormatted = String.format(Locale.US, "%.1f W", watts)
        val voltFormatted = String.format(Locale.US, "%.2f V", stats.voltageVolts)

        // Animated current flow wave frames
        animFrame = (animFrame + 1) % 4
        val flowWave = when (animFrame) {
            0 -> "⚡ »»»» [ $ampFormatted • $wattFormatted ] »»»»"
            1 -> "⚡  »»»  [ $ampFormatted • $wattFormatted ]  »»» "
            2 -> "⚡   »»  [ $ampFormatted • $wattFormatted ]   »» "
            else -> "⚡    ›  [ $ampFormatted • $wattFormatted ]    › "
        }

        val tempStatus = when {
            stats.temperatureCelsius < 38f -> "Cool & Optimal"
            stats.temperatureCelsius < 43f -> "Controlled Flow"
            else -> "Active Management"
        }

        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val modeDisplay = when (mode) {
            ChargeMode.MODE_8A -> "⚡ 8 Ampere Turbo (30W-32W)"
            ChargeMode.MODE_6A -> "⚡ 6 Ampere Stable (22W-25W)"
            ChargeMode.STOCK -> "🛑 Stock OEM Control"
        }

        val bigText = StringBuilder().apply {
            append("$flowWave\n")
            append("────────────────────────────────────────\n")
            append("⏱️ Charging Time  : $timeDisplay (Elapsed)\n")
            append("🌡️ Battery Temp   : ${stats.temperatureCelsius}°C ($tempStatus)\n")
            append("⚡ Real-Time Flow : $ampFormatted ($wattFormatted) • $voltFormatted\n")
            append("🔋 Battery Level  : ${stats.percentage}% (Charging)\n")
            append("🛡️ Active Governor: $modeDisplay\n")
            append("────────────────────────────────────────\n")
            append("🚀 Hyper Charging+ Engine (Build 8) • Tap to Open Hub")
        }.toString()

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_charging)
            .setContentTitle(modeDisplay)
            .setContentText("⚡ $ampFormatted ($wattFormatted)  |  🌡️ ${stats.temperatureCelsius}°C  |  ⏱️ $timeDisplay")
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        try {
            notificationManager.notify(NOTIFICATION_ID, notification)
        } catch (_: Exception) { }
    }

    fun dismissNotification(context: Context) {
        chargeStartTimeMillis = 0L
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        try {
            notificationManager?.cancel(NOTIFICATION_ID)
        } catch (_: Exception) { }
    }

    private fun createChannel(context: Context, manager: NotificationManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val existing = manager.getNotificationChannel(CHANNEL_ID)
            if (existing == null) {
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Live real-time charging status, temperature, wattage, and current flow."
                    setShowBadge(false)
                    enableVibration(false)
                    setSound(null, null)
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
