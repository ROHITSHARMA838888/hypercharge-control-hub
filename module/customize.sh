##########################################################################################
# Hyper Charging+ (with Control Hub Support) - Custom Installer
# All-in-One Installer: KernelSU Fast Charge Module + Auto-Installing Control Hub App
##########################################################################################

SKIPUNZIP=0

ui_print "========================================================"
ui_print "     ⚡ HYPER CHARGING+ (WITH CONTROL HUB SUPPORT) ⚡    "
ui_print "========================================================"
ui_print "- Package Type          : All-in-One Module + App Bundle (Build 8)"
ui_print "- Official Companion App : HyperCharge Control Hub"
ui_print "- App Developed By      : Rohit Sharma (Strict 6A/8A Build)"
ui_print "- Original Module Base  : Demonx4y (Hyper-Charging-Plus)"
ui_print "--------------------------------------------------------"
ui_print "- OS & ROM Support      : HyperOS + AOSP Custom ROMs"
ui_print "- Hardware Target       : ALL Xiaomi & POCO Snapdragon"
ui_print "- 8A Constant Governor  : 8.0A (30W - 32W Steady • Upper Ceiling)"
ui_print "- 6A Stable Governor    : 6.0A (22W - 25W Steady • Solid Floor)"
ui_print "- Master Engine Switch  : Disable / Stock Revert Toggle"
ui_print "- Synchronous Control   : Instant App-to-Daemon Mode Switching"
ui_print "--------------------------------------------------------"
ui_print "- Extracting engine and app files..."

# Universal sysfs detection
if [ -d /sys/class/power_supply/battery ]; then
  ui_print "- Battery sysfs interface detected: OK"
elif [ -d /sys/class/power_supply/qcom-battery ]; then
  ui_print "- Snapdragon battery interface detected: OK"
elif [ -d /sys/class/power_supply/mtk-battery ]; then
  ui_print "- MediaTek battery interface detected: Safe Mode"
else
  ui_print "! Generic power_supply detector: Active"
fi

# Ensure mode.conf exists with default MODE_8A
if [ ! -f "$MODPATH/mode.conf" ]; then
  echo "MODE=MODE_8A" > "$MODPATH/mode.conf"
fi

# Set proper permissions
ui_print "- Configuring permissions & governor daemon..."
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/rohit_daemon.sh" 0 0 0755
set_perm "$MODPATH/mode.conf" 0 0 0666

# ----------------------------------------------------
# All-in-One Auto-Update / Install Companion App
# ----------------------------------------------------
if [ -f "$MODPATH/HyperCharge_Control_Hub.apk" ]; then
  ui_print "--------------------------------------------------------"
  ui_print "- Checking Android Package Manager for App Install..."
  if command -v pm >/dev/null 2>&1 && pm path android >/dev/null 2>&1; then
    ui_print "- Updating HyperCharge Control Hub App directly..."
    pm install -r -d "$MODPATH/HyperCharge_Control_Hub.apk" >/dev/null 2>&1
    if [ $? -eq 0 ]; then
      ui_print "  ✔ HyperCharge Control Hub App Updated Successfully!"
      echo "INSTALLED" > "$MODPATH/.apk_installed_stamp"
      # Auto-grant POST_NOTIFICATIONS runtime permission
      pm grant com.rohit.hypercharge.debug android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
      pm grant com.rohit.hypercharge android.permission.POST_NOTIFICATIONS >/dev/null 2>&1
    else
      ui_print "! Manager install deferred; will auto-install on next boot."
    fi
  else
    ui_print "! Flashed in recovery; app will auto-install on first boot."
  fi
fi

ui_print "--------------------------------------------------------"
ui_print "  ✔ Kernel Module & Companion App Configured!"
ui_print "  ✔ HyperCharge Control Hub hooks active."

# Sync module to active modules directory immediately (bypasses KernelSU reboot requirement)
mkdir -p /data/adb/modules/hyper_charging_plus_controller 2>/dev/null
cp -rf "$MODPATH"/* /data/adb/modules/hyper_charging_plus_controller/ 2>/dev/null
chmod 0755 /data/adb/modules/hyper_charging_plus_controller/rohit_daemon.sh 2>/dev/null
chmod 0755 /data/adb/modules/hyper_charging_plus_controller/service.sh 2>/dev/null

# Start daemon immediately so features work without waiting for reboot
if [ -f "$MODPATH/rohit_daemon.sh" ]; then
  ui_print "- Starting HyperCharge Governor Daemon in background..."
  chmod 0755 "$MODPATH/rohit_daemon.sh"
  nohup sh "$MODPATH/rohit_daemon.sh" >/dev/null 2>&1 &
fi

ui_print "  ✔ Ready! You can reboot anytime to finalize."
ui_print "========================================================"
