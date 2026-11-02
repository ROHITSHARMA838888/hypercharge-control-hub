package com.rohit.hypercharge.util

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import kotlin.math.abs

data class BatteryStats(
    val percentage: Int = 0,
    val temperatureCelsius: Float = 0f,
    val currentMilliAmps: Int = 0,
    val voltageVolts: Float = 0f,
    val isCharging: Boolean = false,
    val statusText: String = "Disconnected"
) {
    val currentAmperes: Float
        get() = currentMilliAmps / 1000f

    val wattageWatts: Float
        get() = if (currentMilliAmps > 0 && voltageVolts > 0) (currentMilliAmps * voltageVolts) / 1000f else 0f
}

enum class ChargeMode(
    val id: String,
    val title: String,
    val subtitle: String,
    val targetMilliAmps: Int,
    val currentAmpsLabel: String
) {
    MODE_8A(
        id = "MODE_8A",
        title = "8 Ampere Turbo",
        subtitle = "⚡ 8.0A Constant Flow (30W - 32W) • Strict Upper Ceiling (Never > 8A)",
        targetMilliAmps = 8000,
        currentAmpsLabel = "8.0A"
    ),
    MODE_6A(
        id = "MODE_6A",
        title = "6 Ampere Stable",
        subtitle = "⚡ 6.0A Constant Flow (22W - 25W) • Solid Floor (6A - 8A Strict Band)",
        targetMilliAmps = 6000,
        currentAmpsLabel = "6.0A"
    ),
    STOCK(
        id = "STOCK",
        title = "Stock OEM Control",
        subtitle = "🛑 All Overrides OFF • Revert to Native System Charging Control",
        targetMilliAmps = -1,
        currentAmpsLabel = "OEM"
    )
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

    suspend fun readBatteryStats(context: Context): BatteryStats = withContext(Dispatchers.IO) {
        var percentage = 0
        var tempC = 0f
        var currentMA = 0
        var voltageV = 0f
        var isCharging = false

        try {
            // 1. Android Native Battery Broadcast (Zero latency, 100% accurate)
            val ifilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            val batteryStatus: Intent? = context.registerReceiver(null, ifilter)
            if (batteryStatus != null) {
                val level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (level != -1 && scale != -1) {
                    percentage = (level * 100 / scale.toFloat()).toInt()
                }
                tempC = batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f
                voltageV = batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000f
                val status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
            }

            // 2. Hardware Fuel Gauge instantaneous current (in microamperes)
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager
            val bmCurrent = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) ?: 0
            if (bmCurrent != 0 && bmCurrent != Integer.MIN_VALUE) {
                val raw = abs(bmCurrent)
                currentMA = if (raw > 100000) raw / 1000 else raw
            }
        } catch (_: Exception) { }

        // 3. Fallback: Direct sysfs file reading (if BatteryManager returns 0)
        if (currentMA == 0) {
            val paths = listOf(
                "/sys/class/power_supply/battery/current_now",
                "/sys/class/power_supply/bms/current_now",
                "/sys/class/power_supply/main/current_now",
                "/sys/class/power_supply/qcom-battery/current_now"
            )
            for (path in paths) {
                try {
                    val f = File(path)
                    if (f.exists() && f.canRead()) {
                        val raw = f.readText().trim().replace("-", "").toIntOrNull() ?: 0
                        if (raw > 0) {
                            currentMA = if (raw > 100000) raw / 1000 else raw
                            break
                        }
                    }
                } catch (_: Exception) { }
            }
        }

        // 4. Root fallback if direct file read was blocked by SELinux
        if (currentMA == 0) {
            try {
                val lines = executeCommand("cat /sys/class/power_supply/battery/current_now /sys/class/power_supply/bms/current_now /sys/class/power_supply/main/current_now 2>/dev/null")
                for (line in lines) {
                    val raw = line.trim().replace("-", "").toIntOrNull() ?: 0
                    if (raw > 0) {
                        currentMA = if (raw > 100000) raw / 1000 else raw
                        break
                    }
                }
            } catch (_: Exception) { }
        }

        // 5. Fallback for percentage & temp if broadcast was null
        if (percentage == 0) {
            try {
                val capFile = File("/sys/class/power_supply/battery/capacity")
                if (capFile.exists() && capFile.canRead()) {
                    percentage = capFile.readText().trim().toIntOrNull() ?: 0
                }
            } catch (_: Exception) { }
        }

        BatteryStats(
            percentage = percentage,
            temperatureCelsius = tempC,
            currentMilliAmps = currentMA,
            voltageVolts = voltageV,
            isCharging = isCharging,
            statusText = if (isCharging) "Charging Active" else "On Battery"
        )
    }

    suspend fun getCurrentMode(): ChargeMode = withContext(Dispatchers.IO) {
        val confPaths = listOf(
            "/data/adb/modules/hyper_charging_plus_controller/mode.conf",
            "/data/adb/modules_update/hyper_charging_plus_controller/mode.conf",
            "/data/adb/modules/rohit_hyper_charge_rebuild/mode.conf",
            "/data/adb/modules/rohits_ultra_fast_charge/mode.conf",
            "/data/adb/modules/Hyper-Charging-Plus/mode.conf"
        )
        for (path in confPaths) {
            val lines = executeCommand("cat $path 2>/dev/null")
            val modeLine = lines.firstOrNull { it.startsWith("MODE=") }
            if (modeLine != null) {
                val modeStr = modeLine.substringAfter("=").trim()
                return@withContext when (modeStr) {
                    "MODE_8A", "FORCE_8A", "8A" -> ChargeMode.MODE_8A
                    "MODE_6A", "TURBO", "FORCE_6A", "6A" -> ChargeMode.MODE_6A
                    "STOCK", "DISABLED" -> ChargeMode.STOCK
                    else -> ChargeMode.MODE_8A
                }
            }
        }
        ChargeMode.MODE_8A
    }

    suspend fun ensurePermissionsAndDaemon(context: Context): Unit = withContext(Dispatchers.IO) {
        val cmds = """
            # 1. Grant POST_NOTIFICATIONS runtime permission via root
            pm grant com.rohit.hypercharge.debug android.permission.POST_NOTIFICATIONS 2>/dev/null
            pm grant com.rohit.hypercharge android.permission.POST_NOTIFICATIONS 2>/dev/null
            
            # 2. KernelSU deferred-flash hot-activation: sync modules_update to modules
            if [ -f "/data/adb/modules_update/hyper_charging_plus_controller/rohit_daemon.sh" ] && [ ! -f "/data/adb/modules/hyper_charging_plus_controller/rohit_daemon.sh" ]; then
                mkdir -p /data/adb/modules/hyper_charging_plus_controller
                cp -rf /data/adb/modules_update/hyper_charging_plus_controller/* /data/adb/modules/hyper_charging_plus_controller/ 2>/dev/null
                chmod 0755 /data/adb/modules/hyper_charging_plus_controller/rohit_daemon.sh 2>/dev/null
                chmod 0755 /data/adb/modules/hyper_charging_plus_controller/service.sh 2>/dev/null
            fi
            
            # 3. Ensure rohit_daemon.sh is actively running
            if ! pgrep -f "rohit_daemon.sh" >/dev/null 2>&1; then
                for d in \
                    /data/adb/modules/hyper_charging_plus_controller/rohit_daemon.sh \
                    /data/adb/modules_update/hyper_charging_plus_controller/rohit_daemon.sh \
                    /data/adb/modules/rohit_hyper_charge_rebuild/rohit_daemon.sh; do
                    if [ -f "${'$'}d" ]; then
                        chmod 0755 "${'$'}d" 2>/dev/null
                        nohup sh "${'$'}d" >/dev/null 2>&1 &
                        break
                    fi
                done
            fi
        """.trimIndent()
        executeCommand(cmds)
    }

    suspend fun applyMode(mode: ChargeMode): Boolean = withContext(Dispatchers.IO) {
        val confPaths = listOf(
            "/data/adb/modules/hyper_charging_plus_controller/mode.conf",
            "/data/adb/modules_update/hyper_charging_plus_controller/mode.conf",
            "/data/adb/modules/rohit_hyper_charge_rebuild/mode.conf",
            "/data/adb/modules/rohits_ultra_fast_charge/mode.conf",
            "/data/adb/modules/Hyper-Charging-Plus/mode.conf"
        )
        try {
            val sb = StringBuilder()

            // 1. Write mode.conf synchronously across all module directories with full rw permissions
            sb.append("mkdir -p /data/adb/modules/hyper_charging_plus_controller 2>/dev/null\n")
            for (path in confPaths) {
                sb.append("echo \"MODE=${mode.id}\" > $path 2>/dev/null\n")
                sb.append("chmod 0666 $path 2>/dev/null\n")
            }

            // 2. Grant notification permission
            sb.append("pm grant com.rohit.hypercharge.debug android.permission.POST_NOTIFICATIONS 2>/dev/null\n")
            sb.append("pm grant com.rohit.hypercharge android.permission.POST_NOTIFICATIONS 2>/dev/null\n")

            // 3. Apply exact hardware registers synchronously based on selected mode
            when (mode) {
                ChargeMode.MODE_8A -> {
                    // ====================================================
                    // 8 Ampere Turbo Mode (30W - 32W Constant Flow)
                    // Hard Ceiling: Exactly 8000000 uA (Never > 8A)
                    // ====================================================
                    sb.append("pkill -STOP mi_thermald 2>/dev/null\n")
                    sb.append("pkill -STOP thermal-engine 2>/dev/null\n")

                    // Xiaomi MCA Hardware Clamp
                    sb.append("echo 1 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_thermal_remove 2>/dev/null\n")
                    sb.append("echo 8000000 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr 2>/dev/null\n")
                    sb.append("echo 8000000 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr2 2>/dev/null\n")
                    sb.append("echo 1 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_ctrl_limit 2>/dev/null\n")

                    // Battery nodes
                    sb.append("chmod 666 /sys/class/power_supply/battery/constant_charge_current_max 2>/dev/null\n")
                    sb.append("echo 8000000 > /sys/class/power_supply/battery/constant_charge_current_max 2>/dev/null\n")
                    sb.append("echo 8000000 > /sys/class/power_supply/battery/current_max 2>/dev/null\n")
                    sb.append("echo 8000000 > /sys/class/power_supply/battery/fcc 2>/dev/null\n")

                    // 2.0A @ 16V = 32W input bus -> strictly 8.0A battery charge flow
                    sb.append("echo 2000000 > /sys/class/power_supply/battery/input_current_limit 2>/dev/null\n")
                    sb.append("echo 2000000 > /sys/class/power_supply/usb/current_max 2>/dev/null\n")
                    sb.append("echo 2000000 > /sys/class/power_supply/usb/input_current_limit 2>/dev/null\n")

                    // Thermal / Control limits
                    sb.append("echo 1 > /sys/class/power_supply/battery/system_temp_level 2>/dev/null\n")
                    sb.append("echo 1 > /sys/class/qcom-battery/system_temp_level 2>/dev/null\n")
                    sb.append("echo 1 > /sys/class/power_supply/battery/charge_control_limit 2>/dev/null\n")
                    sb.append("echo 1 > /sys/class/power_supply/battery/force_fast_charge 2>/dev/null\n")
                    sb.append("echo 1 > /sys/class/power_supply/battery/fastcharge_mode 2>/dev/null\n")
                    sb.append("echo 1 > /sys/class/power_supply/battery/hypercharge_mode 2>/dev/null\n")

                    // Charge pump config for 8A
                    sb.append("echo 1 > /sys/class/power_supply/charge_pump/cp_charging_enabled 2>/dev/null\n")
                    sb.append("echo 2000000 > /sys/class/power_supply/charge_pump/current_max 2>/dev/null\n")
                    sb.append("echo 2000000 > /sys/class/power_supply/charge_pump/ibus_max 2>/dev/null\n")
                    sb.append("echo 2000000 > /sys/class/power_supply/charge_pump/ibus_lmt 2>/dev/null\n")
                    sb.append("echo 2000000 > /sys/class/power_supply/cp_master/current_max 2>/dev/null\n")
                }
                ChargeMode.MODE_6A -> {
                    // ====================================================
                    // 6 Ampere Stable Mode (22W - 25W Constant Flow)
                    // Solid Floor & Controlled Flow: Exactly 6000000 uA
                    // ====================================================
                    sb.append("pkill -STOP mi_thermald 2>/dev/null\n")
                    sb.append("pkill -STOP thermal-engine 2>/dev/null\n")

                    // Xiaomi MCA Hardware Clamp
                    sb.append("echo 1 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_thermal_remove 2>/dev/null\n")
                    sb.append("echo 6000000 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr 2>/dev/null\n")
                    sb.append("echo 6000000 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr2 2>/dev/null\n")
                    sb.append("echo 2 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_ctrl_limit 2>/dev/null\n")

                    // Battery nodes
                    sb.append("chmod 666 /sys/class/power_supply/battery/constant_charge_current_max 2>/dev/null\n")
                    sb.append("echo 6000000 > /sys/class/power_supply/battery/constant_charge_current_max 2>/dev/null\n")
                    sb.append("echo 6000000 > /sys/class/power_supply/battery/current_max 2>/dev/null\n")
                    sb.append("echo 6000000 > /sys/class/power_supply/battery/fcc 2>/dev/null\n")

                    // 1.5A @ 16V = 24W input bus -> strictly 6.0A battery charge flow
                    sb.append("echo 1500000 > /sys/class/power_supply/battery/input_current_limit 2>/dev/null\n")
                    sb.append("echo 1500000 > /sys/class/power_supply/usb/current_max 2>/dev/null\n")
                    sb.append("echo 1500000 > /sys/class/power_supply/usb/input_current_limit 2>/dev/null\n")

                    // Thermal / Control limits
                    sb.append("echo 2 > /sys/class/power_supply/battery/system_temp_level 2>/dev/null\n")
                    sb.append("echo 2 > /sys/class/qcom-battery/system_temp_level 2>/dev/null\n")
                    sb.append("echo 2 > /sys/class/power_supply/battery/charge_control_limit 2>/dev/null\n")
                    sb.append("echo 1 > /sys/class/power_supply/battery/force_fast_charge 2>/dev/null\n")
                    sb.append("echo 1 > /sys/class/power_supply/battery/fastcharge_mode 2>/dev/null\n")
                    sb.append("echo 1 > /sys/class/power_supply/battery/hypercharge_mode 2>/dev/null\n")

                    // Charge pump config for 6A
                    sb.append("echo 1 > /sys/class/power_supply/charge_pump/cp_charging_enabled 2>/dev/null\n")
                    sb.append("echo 1500000 > /sys/class/power_supply/charge_pump/current_max 2>/dev/null\n")
                    sb.append("echo 1500000 > /sys/class/power_supply/charge_pump/ibus_max 2>/dev/null\n")
                    sb.append("echo 1500000 > /sys/class/power_supply/charge_pump/ibus_lmt 2>/dev/null\n")
                    sb.append("echo 1500000 > /sys/class/power_supply/cp_master/current_max 2>/dev/null\n")
                }
                ChargeMode.STOCK -> {
                    // ====================================================
                    // Stock OEM Control Mode (Disable all overrides)
                    // ====================================================
                    sb.append("pkill -CONT mi_thermald 2>/dev/null\n")
                    sb.append("pkill -CONT thermal-engine 2>/dev/null\n")
                    sb.append("echo 0 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_thermal_remove 2>/dev/null\n")
                    sb.append("echo 0 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr 2>/dev/null\n")
                    sb.append("echo 0 > /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr2 2>/dev/null\n")
                }
            }

            // 4. Ensure daemon is running and wake it up immediately to ingest new mode without delay
            sb.append("""
                if pgrep -f "rohit_daemon.sh" >/dev/null 2>&1; then
                    pkill -HUP -f "rohit_daemon.sh" 2>/dev/null
                else
                    for d_path in \
                        /data/adb/modules/hyper_charging_plus_controller/rohit_daemon.sh \
                        /data/adb/modules_update/hyper_charging_plus_controller/rohit_daemon.sh; do
                        if [ -f "${'$'}d_path" ]; then
                            chmod 0755 "${'$'}d_path" 2>/dev/null
                            nohup sh "${'$'}d_path" >/dev/null 2>&1 &
                            break
                        fi
                    done
                fi
            """.trimIndent() + "\n")

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
