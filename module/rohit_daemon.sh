#!/system/bin/sh
##########################################################################################
# Hyper Charging+ (with Control Hub Support) - Core Governor Daemon
# Companion App Developed by : Rohit Sharma
# Version                     : Build 8 (Strict 6A/8A Lock & Instant Mode Binding)
# Compatibility               : ALL Xiaomi & POCO Snapdragon (HyperOS & AOSP)
##########################################################################################

MODDIR=${0%/*}

# Locate Battery power_supply directory
BATT=""
for path in /sys/class/power_supply/battery /sys/class/power_supply/qcom-battery /sys/class/power_supply/bms /sys/class/power_supply/mtk-battery; do
    if [ -d "$path" ]; then
        BATT="$path"
        break
    fi
done

[ -z "$BATT" ] && BATT="/sys/class/power_supply/battery"

# Safe write helper: only writes if file exists, never attempts to create files on read-only sysfs
safe_write() {
    val="$1"
    shift
    for target in "$@"; do
        if [ -f "$target" ]; then
            chmod 666 "$target" 2>/dev/null
            echo "$val" > "$target" 2>/dev/null
        fi
    done
}

# Configuration file paths for HyperCharge Control Hub App
CONF_PATHS="/data/adb/modules/hyper_charging_plus_controller/mode.conf /data/adb/modules_update/hyper_charging_plus_controller/mode.conf /data/adb/modules/rohit_hyper_charge_rebuild/mode.conf /data/adb/modules/rohits_ultra_fast_charge/mode.conf /data/adb/modules/Hyper-Charging-Plus/mode.conf $MODDIR/mode.conf"

NOTIF_TAG="rohit_hyper_charge"
WAS_CHARGING=0
CHARGE_START_EPOCH=0
ANIM_FRAME=0

# Proactively grant notification runtime permission to companion apps via root
pm grant com.rohit.hypercharge.debug android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
pm grant com.rohit.hypercharge android.permission.POST_NOTIFICATIONS >/dev/null 2>&1

# SIGHUP Trap: Instant mode reload on app button press
trap ':' HUP

# Main Governor Loop
while true; do
    # Check charging status (robust case-insensitive and USB online check)
    IS_CHARGING=0
    if [ -f "$BATT/status" ]; then
        STATUS=$(cat "$BATT/status" 2>/dev/null)
        case "$STATUS" in
            *[Cc]harg*|*[Ff]ull*) IS_CHARGING=1 ;;
        esac
    fi
    if [ "$IS_CHARGING" -eq 0 ]; then
        for u_online in /sys/class/power_supply/usb/online /sys/class/power_supply/battery/present; do
            if [ -f "$u_online" ] && [ "$(cat "$u_online" 2>/dev/null)" = "1" ]; then
                IS_CHARGING=1
                break
            fi
        done
    fi

    # Read Active Mode from Configuration File synchronously at loop start
    ACTIVE_MODE="MODE_8A"
    for conf in $CONF_PATHS; do
        if [ -f "$conf" ]; then
            CONF_MODE=$(grep -E "^MODE=" "$conf" 2>/dev/null | cut -d'=' -f2 | tr -d ' \r\n')
            if [ -n "$CONF_MODE" ]; then
                ACTIVE_MODE="$CONF_MODE"
                break
            fi
        fi
    done

    # ----------------------------------------------------
    # Check for Master Stock / Disabled Mode
    # ----------------------------------------------------
    if [ "$ACTIVE_MODE" = "STOCK" ] || [ "$ACTIVE_MODE" = "DISABLED" ]; then
        pkill -CONT mi_thermald 2>/dev/null
        pkill -CONT thermal-engine 2>/dev/null
        safe_write 0 \
            /sys/bus/platform/devices/soc:mca_charger_thermal/wired_thermal_remove \
            /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr \
            /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr2

        if [ "$IS_CHARGING" -eq 1 ]; then
            cmd notification post -S bigtext \
                -t "🛑 HyperCharge Hub: Stock OEM Control" \
                "$NOTIF_TAG" \
                "Engine Status: Stock Native Charging Active\nAll custom charging overrides are turned OFF.\nHardware and thermal governors are in native OEM mode." 2>/dev/null
            WAS_CHARGING=1
        else
            if [ "$WAS_CHARGING" -eq 1 ]; then
                cmd notification cancel "$NOTIF_TAG" 2>/dev/null
                WAS_CHARGING=0
            fi
        fi
        sleep 2 & wait $!
        continue
    fi

    # ----------------------------------------------------
    # Active Charging Governor Branch
    # ----------------------------------------------------
    if [ "$IS_CHARGING" -eq 1 ]; then
        # Read battery capacity
        CAPACITY=$(cat "$BATT/capacity" 2>/dev/null)
        [ -z "$CAPACITY" ] && CAPACITY="--"

        # Read battery temperature
        RAW_TEMP=$(cat "$BATT/temp" 2>/dev/null)
        if [ -n "$RAW_TEMP" ]; then
            if [ "$RAW_TEMP" -gt 10000 ]; then
                TEMP_C=$((RAW_TEMP / 1000))
            elif [ "$RAW_TEMP" -gt 100 ]; then
                TEMP_C=$((RAW_TEMP / 10))
            else
                TEMP_C=$RAW_TEMP
            fi
        else
            TEMP_C=30
        fi

        # Read actual real-time current for telemetry & closed-loop governor
        REAL_CURRENT=$(cat "$BATT/current_now" 2>/dev/null)
        CURRENT_MA=0
        if [ -n "$REAL_CURRENT" ]; then
            REAL_CURRENT=$(echo "$REAL_CURRENT" | sed 's/-//')
            if [ "$REAL_CURRENT" -gt 10000 ]; then
                CURRENT_MA=$((REAL_CURRENT / 1000))
            else
                CURRENT_MA=$REAL_CURRENT
            fi
        fi

        # ----------------------------------------------------
        # Mode Resolution Logic (Strict 6A / 8A Binding)
        # TARGET_MODE:
        #   8A -> 8.0A Turbo (30W-32W) • Strict Ceiling (Never > 8A)
        #   6A -> 6.0A Stable (22W-25W) • Solid Floor (Strict 6A - 8A Band)
        # ----------------------------------------------------
        case "$ACTIVE_MODE" in
            MODE_6A|TURBO|FORCE_6A|6A)
                MODE_NAME="6 Ampere Stable (22W-25W)"
                TARGET_TIER=6
                ;;
            *)
                # Default is 8 Ampere Turbo
                MODE_NAME="8 Ampere Turbo (30W-32W)"
                TARGET_TIER=8
                ;;
        esac

        # ----------------------------------------------------
        # Hardware Clamping & Enforcement
        # ----------------------------------------------------
        pkill -STOP mi_thermald 2>/dev/null
        pkill -STOP thermal-engine 2>/dev/null

        if [ "$TARGET_TIER" -eq 8 ]; then
            # ====================================================
            # 8 AMPERE TURBO (30W - 32W CONSTANT FLOW)
            # Absolute Upper Ceiling: Exactly 8.0A (8000000 uA)
            # ====================================================
            safe_write 1 /sys/bus/platform/devices/soc:mca_charger_thermal/wired_thermal_remove
            safe_write 8000000 \
                /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr \
                /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr2 \
                "$BATT/constant_charge_current_max" \
                "$BATT/current_max" \
                "$BATT/fcc"
            safe_write 1 /sys/bus/platform/devices/soc:mca_charger_thermal/wired_ctrl_limit

            # 2.0A @ 16V = 32W input bus -> strictly 8.0A battery charge flow
            safe_write 2000000 \
                "$BATT/input_current_limit" \
                /sys/class/power_supply/usb/current_max \
                /sys/class/power_supply/usb/input_current_limit

            safe_write 1 \
                /sys/class/power_supply/charge_pump/cp_charging_enabled \
                /sys/class/power_supply/cp-master/cp_charging_enabled \
                /sys/class/power_supply/sc8551-standalone/cp_charging_enabled \
                /sys/class/power_supply/ln8000/cp_charging_enabled
            safe_write 1 "$BATT/force_fast_charge" "$BATT/fastcharge_mode" "$BATT/hypercharge_mode" "$BATT/smart_chg"
            safe_write 1 "$BATT/system_temp_level" /sys/class/qcom-battery/system_temp_level "$BATT/charge_control_limit"

            safe_write 2000000 \
                /sys/class/power_supply/charge_pump/current_max \
                /sys/class/power_supply/charge_pump/ibus_max \
                /sys/class/power_supply/charge_pump/ibus_lmt \
                /sys/class/power_supply/cp_master/current_max

            # Scoped Closed-Loop Trimming: Keeps flow pegged between 7800mA - 8000mA
            # NEVER writes higher than 8000000 uA!
            if [ "$CURRENT_MA" -gt 8050 ]; then
                safe_write 7800000 \
                    "$BATT/constant_charge_current_max" \
                    /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr
                safe_write 1950000 "$BATT/input_current_limit"
            elif [ "$CURRENT_MA" -lt 7700 ] && [ "$CURRENT_MA" -gt 0 ]; then
                safe_write 8000000 \
                    "$BATT/constant_charge_current_max" \
                    /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr
                safe_write 2000000 "$BATT/input_current_limit"
            fi

        elif [ "$TARGET_TIER" -eq 6 ]; then
            # ====================================================
            # 6 AMPERE STABLE (22W - 25W CONSTANT FLOW)
            # Solid Floor & Strict Ceiling: Exactly 6.0A (6000000 uA)
            # ====================================================
            safe_write 1 /sys/bus/platform/devices/soc:mca_charger_thermal/wired_thermal_remove
            safe_write 6000000 \
                /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr \
                /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr2 \
                "$BATT/constant_charge_current_max" \
                "$BATT/current_max" \
                "$BATT/fcc"
            safe_write 2 /sys/bus/platform/devices/soc:mca_charger_thermal/wired_ctrl_limit

            # 1.5A @ 16V = 24W input bus -> strictly 6.0A battery charge flow
            safe_write 1500000 \
                "$BATT/input_current_limit" \
                /sys/class/power_supply/usb/current_max \
                /sys/class/power_supply/usb/input_current_limit

            safe_write 1 \
                /sys/class/power_supply/charge_pump/cp_charging_enabled \
                /sys/class/power_supply/cp-master/cp_charging_enabled \
                /sys/class/power_supply/sc8551-standalone/cp_charging_enabled \
                /sys/class/power_supply/ln8000/cp_charging_enabled
            safe_write 1 "$BATT/force_fast_charge" "$BATT/fastcharge_mode" "$BATT/hypercharge_mode" "$BATT/smart_chg"
            safe_write 2 "$BATT/system_temp_level" /sys/class/qcom-battery/system_temp_level "$BATT/charge_control_limit"

            safe_write 1500000 \
                /sys/class/power_supply/charge_pump/current_max \
                /sys/class/power_supply/charge_pump/ibus_max \
                /sys/class/power_supply/charge_pump/ibus_lmt \
                /sys/class/power_supply/cp_master/current_max

            # Scoped Closed-Loop Trimming: Keeps flow pegged between 5800mA - 6000mA
            # NEVER writes higher than 6000000 uA!
            if [ "$CURRENT_MA" -gt 6050 ]; then
                safe_write 5800000 \
                    "$BATT/constant_charge_current_max" \
                    /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr
                safe_write 1450000 "$BATT/input_current_limit"
            elif [ "$CURRENT_MA" -lt 5700 ] && [ "$CURRENT_MA" -gt 0 ]; then
                safe_write 6000000 \
                    "$BATT/constant_charge_current_max" \
                    /sys/bus/platform/devices/soc:mca_charger_thermal/wired_chg_curr
                safe_write 1500000 "$BATT/input_current_limit"
            fi
        fi

        # ----------------------------------------------------
        # Real-Time Telemetry & Elapsed Time Calculation
        # ----------------------------------------------------
        if [ "$WAS_CHARGING" -eq 0 ] || [ "$CHARGE_START_EPOCH" -eq 0 ]; then
            CHARGE_START_EPOCH=$(date +%s 2>/dev/null)
            [ -z "$CHARGE_START_EPOCH" ] && CHARGE_START_EPOCH=0
            WAS_CHARGING=1
        fi

        NOW_EPOCH=$(date +%s 2>/dev/null)
        [ -z "$NOW_EPOCH" ] && NOW_EPOCH=0
        ELAPSED_SEC=$((NOW_EPOCH - CHARGE_START_EPOCH))
        [ "$ELAPSED_SEC" -lt 0 ] && ELAPSED_SEC=0
        ELAPSED_MIN=$((ELAPSED_SEC / 60))
        ELAPSED_REM_SEC=$((ELAPSED_SEC % 60))
        if [ "$ELAPSED_MIN" -ge 60 ]; then
            ELAPSED_HR=$((ELAPSED_MIN / 60))
            ELAPSED_REM_MIN=$((ELAPSED_MIN % 60))
            TIME_DISPLAY=$(printf "%dh %02dm" "$ELAPSED_HR" "$ELAPSED_REM_MIN")
        else
            TIME_DISPLAY=$(printf "%02dm %02ds" "$ELAPSED_MIN" "$ELAPSED_REM_SEC")
        fi

        # Voltage calculation
        VOLT_RAW=$(cat "$BATT/voltage_now" 2>/dev/null)
        VOLT_MV=0
        if [ -n "$VOLT_RAW" ]; then
            if [ "$VOLT_RAW" -gt 100000 ]; then
                VOLT_MV=$((VOLT_RAW / 1000))
            else
                VOLT_MV=$VOLT_RAW
            fi
        fi

        if [ "$CURRENT_MA" -gt 0 ] && [ "$VOLT_MV" -gt 0 ]; then
            WATT_TENTHS=$(( (CURRENT_MA * VOLT_MV) / 100000 ))
            W_INT=$((WATT_TENTHS / 10))
            W_DEC=$((WATT_TENTHS % 10))
            POWER_DISPLAY="${W_INT}.${W_DEC}W (${CURRENT_MA}mA)"
        elif [ "$CURRENT_MA" -gt 0 ]; then
            POWER_DISPLAY="${CURRENT_MA}mA"
        else
            POWER_DISPLAY="Monitoring flow..."
        fi

        # Temperature status tag
        if [ "$TEMP_C" -lt 38 ]; then
            TEMP_STATUS="Cool & Optimal"
        elif [ "$TEMP_C" -lt 43 ]; then
            TEMP_STATUS="Controlled Flow"
        else
            TEMP_STATUS="Active Management"
        fi

        # Animated current flow wave frames
        ANIM_FRAME=$(( (ANIM_FRAME + 1) % 4 ))
        case "$ANIM_FRAME" in
            0) FLOW_WAVE="⚡ »»»» [ ${POWER_DISPLAY} ] »»»»" ;;
            1) FLOW_WAVE="⚡  »»»  [ ${POWER_DISPLAY} ]  »»» " ;;
            2) FLOW_WAVE="⚡   »»  [ ${POWER_DISPLAY} ]   »» " ;;
            *) FLOW_WAVE="⚡    ›  [ ${POWER_DISPLAY} ]    › " ;;
        esac

        # ----------------------------------------------------
        # Live Notification Center UI Panel (100% Native & Crash-Proof)
        # ----------------------------------------------------
        cmd notification post -S bigtext \
            -t "⚡ $MODE_NAME" \
            "$NOTIF_TAG" \
            "$FLOW_WAVE
⏱️ Time: $TIME_DISPLAY  |  🌡️ Temp: ${TEMP_C}°C ($TEMP_STATUS)
⚡ Real-Time Power: $POWER_DISPLAY  |  🔋 Batt: ${CAPACITY}%
🛡️ Engine: Hyper Charging+ Control Hub (Build 8 Active)" 2>/dev/null

        sleep 1.5 & wait $!
    else
        # When unplugged, resume thermal daemons and dismiss status bar notification
        if [ "$WAS_CHARGING" -eq 1 ]; then
            pkill -CONT mi_thermald 2>/dev/null
            pkill -CONT thermal-engine 2>/dev/null
            safe_write 0 /sys/bus/platform/devices/soc:mca_charger_thermal/wired_thermal_remove
            cmd notification cancel "$NOTIF_TAG" 2>/dev/null
            WAS_CHARGING=0
            CHARGE_START_EPOCH=0
        fi
        sleep 3 & wait $!
    fi
done
