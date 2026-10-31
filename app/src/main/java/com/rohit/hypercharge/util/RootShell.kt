package com.rohit.hypercharge.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

data class BatteryStats(
    val percentage: Int = 0,
    val temperatureCelsius: Float = 0f,
    val currentMilliAmps: Int = 0,
    val voltageVolts: Float = 0f,
    val isCharging: Boolean = false,
    val statusText: String = "Disconnected"
)

enum class ChargeMode(val id: String, val title: String, val subtitle: String, val targetMilliAmps: Int) {
    TURBO("TURBO", "Ultra Turbo", "6.0A Peak Speed • Cooler Recommended", 6000),
    BALANCED("BALANCED", "Balanced", "4.5A Fast Charge • Daily Balanced Use", 4500),
    ECO("ECO", "Eco Battery Health", "2.5A Cool Charge • Minimal Heat", 2500),
    BYPASS("BYPASS", "Gaming Bypass", "0.5A Idle Charge • Stops Gaming Heat", 500),
    AUTO("AUTO", "Auto Governor", "Dynamic Ladder • Smart Thermal Throttling", 0)
}

object RootShell {

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("id\nexit\n")
                os.flush()
            }
            process.waitFor() == 0
        } catch (_: Exception) {
            false
        }
    }

    suspend fun readBatteryStats(): BatteryStats = withContext(Dispatchers.IO) {
        var percentage = 0
        var tempC = 0f
        var currentMA = 0
        var voltageV = 0f
        var status = "Discharging"

        try {
            val cmd = """
                cat /sys/class/power_supply/battery/capacity 2>/dev/null
                cat /sys/class/power_supply/battery/temp 2>/dev/null
                cat /sys/class/power_supply/battery/current_now 2>/dev/null
                cat /sys/class/power_supply/battery/voltage_now 2>/dev/null
                cat /sys/class/power_supply/battery/status 2>/dev/null
            """.trimIndent()

            val lines = executeCommand(cmd)
            if (lines.size >= 5) {
                percentage = lines[0].trim().toIntOrNull() ?: 0
                val rawTemp = lines[1].trim().toFloatOrNull() ?: 0f
                tempC = when {
                    rawTemp > 10000 -> rawTemp / 1000f
                    rawTemp > 100 -> rawTemp / 10f
                    else -> rawTemp
                }

                val rawCurrent = lines[2].trim().replace("-", "").toIntOrNull() ?: 0
                currentMA = if (rawCurrent > 100000) rawCurrent / 1000 else rawCurrent

                val rawVoltage = lines[3].trim().toFloatOrNull() ?: 0f
                voltageV = if (rawVoltage > 1000000) rawVoltage / 1000000f else rawVoltage / 1000f

                status = lines[4].trim()
            }
        } catch (_: Exception) { }

        BatteryStats(
            percentage = percentage,
            temperatureCelsius = tempC,
            currentMilliAmps = currentMA,
            voltageVolts = voltageV,
            isCharging = status.equals("Charging", ignoreCase = true),
            statusText = if (status.equals("Charging", ignoreCase = true)) "Fast Charging Active" else status
        )
    }

    suspend fun getCurrentMode(): ChargeMode = withContext(Dispatchers.IO) {
        val confPaths = listOf(
            "/data/adb/modules/rohit_hyper_charge_rebuild/mode.conf",
            "/data/adb/modules/rohits_ultra_fast_charge/mode.conf"
        )
        for (path in confPaths) {
            val lines = executeCommand("cat $path 2>/dev/null")
            val modeLine = lines.firstOrNull { it.startsWith("MODE=") }
            if (modeLine != null) {
                val modeStr = modeLine.substringAfter("=").trim()
                return@withContext ChargeMode.entries.firstOrNull { it.id == modeStr } ?: ChargeMode.AUTO
            }
        }
        ChargeMode.AUTO
    }

    suspend fun applyMode(mode: ChargeMode): Boolean = withContext(Dispatchers.IO) {
        val confPaths = listOf(
            "/data/adb/modules/rohit_hyper_charge_rebuild/mode.conf",
            "/data/adb/modules/rohits_ultra_fast_charge/mode.conf"
        )
        try {
            val sb = StringBuilder()
            for (path in confPaths) {
                sb.append("echo \"MODE=${mode.id}\" > $path 2>/dev/null\n")
            }
            if (mode.targetMilliAmps > 0) {
                val targetUA = mode.targetMilliAmps * 1000
                sb.append("echo $targetUA > /sys/class/power_supply/battery/constant_charge_current_max 2>/dev/null\n")
                sb.append("echo $targetUA > /sys/class/power_supply/battery/current_max 2>/dev/null\n")
            }
            executeCommand(sb.toString())
            true
        } catch (_: Exception) {
            false
        }
    }

    private fun executeCommand(command: String): List<String> {
        val output = mutableListOf<String>()
        try {
            val process = Runtime.getRuntime().exec("su")
            DataOutputStream(process.outputStream).use { os ->
                os.writeBytes("$command\nexit\n")
                os.flush()
            }
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    line?.let { output.add(it) }
                }
            }
            process.waitFor()
        } catch (_: Exception) { }
        return output
    }
}
