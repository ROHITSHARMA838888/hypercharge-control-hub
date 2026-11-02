#!/system/bin/sh
MODDIR=${0%/*}

# Wait until Android has finished booting
until [ "$(getprop sys.boot_completed)" = "1" ]; do
    sleep 3
done

# Sleep an extra 5 seconds to ensure system services (PackageManager, etc.) are up
sleep 5

# ----------------------------------------------------
# All-in-One Companion App Auto-Installer / Updater
# ----------------------------------------------------
if [ -f "$MODDIR/HyperCharge_Control_Hub.apk" ]; then
    # If not yet installed or updated via customize.sh, install now
    if [ ! -f "$MODDIR/.apk_installed_stamp" ]; then
        pm install -r -d "$MODDIR/HyperCharge_Control_Hub.apk" >/dev/null 2>&1
        if [ $? -eq 0 ]; then
            echo "INSTALLED" > "$MODDIR/.apk_installed_stamp"
        fi
    fi
fi

# Launch Rohit's Ultra Fast Charging Engine Daemon in background
if [ -f "$MODDIR/rohit_daemon.sh" ]; then
    chmod 0755 "$MODDIR/rohit_daemon.sh"
    nohup sh "$MODDIR/rohit_daemon.sh" >/dev/null 2>&1 &
fi
