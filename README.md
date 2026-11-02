# ⚡ HyperCharge Control Hub (Unified Edition)

[![Developer: Rohit Sharma](https://img.shields.io/badge/Developed%20By-Rohit%20Sharma-blue?style=for-the-badge&logo=android)](https://github.com/ROHITSHARMA838888)
[![Project: Unified Monorepo](https://img.shields.io/badge/Architecture-Unified%20Monorepo-cyan?style=for-the-badge&logo=git)](https://github.com/ROHITSHARMA838888/hypercharge-control-hub)
[![Kernel: KernelSU / Magisk / APatch](https://img.shields.io/badge/Root-KernelSU%20%2F%20Magisk%20%2F%20APatch-red?style=for-the-badge&logo=android)](https://github.com/ROHITSHARMA838888/hypercharge-control-hub)
[![OS: HyperOS & AOSP](https://img.shields.io/badge/OS-HyperOS%20%2B%20AOSP-success?style=for-the-badge&logo=android)](https://github.com/ROHITSHARMA838888/hypercharge-control-hub)
[![Hardware: Xiaomi & POCO Snapdragon](https://img.shields.io/badge/Hardware-Xiaomi%20%26%20POCO%20Snapdragon-orange?style=for-the-badge)](https://github.com/ROHITSHARMA838888/hypercharge-control-hub)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-green.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)

**HyperCharge Control Hub** is the all-in-one charging management engine and manual controller application designed and developed by **Rohit Sharma** for **Xiaomi & POCO Qualcomm Snapdragon** devices (POCO F-series, Redmi K-series, Redmi Note series, Xiaomi Flagships) across both **HyperOS** and **AOSP Custom ROMs** (Android 14, 15, and 17).

---

## 🏗️ Unified Monorepo Architecture

This repository unifies the entire ecosystem under one roof — combining the low-level **KernelSU Governor Engine** with the modern **Jetpack Compose Android Companion App**:

```
HyperCharge-Control-Hub/
├── app/                  # Android Companion App (Kotlin, Jetpack Compose, Material3)
├── module/               # KernelSU / Magisk Charging Module (Shell Governor & Hooks)
│   ├── META-INF/         # Magisk/KernelSU update-binary & updater-script
│   ├── customize.sh      # Flash installer & direct APK package manager
│   ├── service.sh        # Boot-completed daemon launcher
│   ├── rohit_daemon.sh   # Closed-loop hardware current governor
│   ├── module.prop       # Module metadata
│   └── mode.conf         # Active governor configuration
├── build_all.ps1         # One-click unified build script (Compiles APK + Packages ZIPs)
└── README.md             # Complete project documentation
```

---

## ⚡ What's New in Build 08: Strict 6A / 8A Lock

1. **Permanent Removal of Old 10A/13A Options:**
   * All 10A, 13A, uncalibrated force toggles, and aggressive thermal cooldown loops have been permanently stripped from both the UI and the kernel daemon.
   * Hard ceiling is pegged strictly at **8.0A (8000000 µA)**. The charging current will **never exceed 8A under any circumstance**.

2. **Two Dedicated Fixed Governor Buttons:**
   * ⚡ **8 Ampere Turbo (`MODE_8A`):**
     * **8.0A Constant Flow (30W – 32W).**
     * Hardware hard ceiling cap: 8000000 µA ($8\text{A}$).
     * Maintains maximum safe fast-charging speed.
   * ⚡ **6 Ampere Stable (`MODE_6A`):**
     * **6.0A Constant Flow (22W – 25W).**
     * Solid floor (never drop below 6A while active, never exceed 8A).
     * Minimal battery warmth with rock-solid charging speed.
   * 🛑 **Master Engine Switch:**
     * Toggle between **Active** and **Stock (OEM Control)** to turn OFF all overrides in one tap.

3. **Instant App-to-Daemon Mode Switching (Bug Fixed):**
   * Mode transitions (switching between 8A and 6A) now take effect **instantly with zero delay**.
   * Daemon SIGHUP wake-up ensures the closed-loop governor reloads immediately without fighting previous limits.

4. **Notification Center Live Telemetry Panel:**
   * Live charging telemetry directly inside the Android 15 & 17 Notification Shade.
   * Displays real-time Amperage ($A$), Wattage ($W$), Voltage ($V$), Battery Temperature ($°C$), Elapsed Charging Time, and an animated current flow wave.

---

## 🚀 One-Click Build & Packaging

Build the Android APK and both flashable KernelSU modules in a single step using PowerShell:

```powershell
.\build_all.ps1
```

This automatically generates the following output in the `dist/` directory:
1. `HyperCharge_Control_Hub_Build-08.apk` — Standalone Android App
2. `HyperCharge_AllInOne_Module_Build-08.zip` — Complete module with auto-installing companion app
3. `HyperCharge_ModuleOnly_Build-08.zip` — KernelSU module only (without embedded APK)

---

## 📱 Installation Instructions

### Method 1: All-In-One (Recommended)
1. Transfer `HyperCharge_AllInOne_Module_Build-08.zip` to your phone.
2. Open **KernelSU** or **Magisk** ➔ **Modules** ➔ **Install from storage** ➔ Select the ZIP.
3. Reboot your phone after flashing.
4. Open **HyperCharge Control Hub**, grant Root permission, and select your preferred mode (**8 Ampere Turbo** or **6 Ampere Stable**).

### Method 2: Separate Install
1. Install `HyperCharge_Control_Hub_Build-08.apk` directly on your phone.
2. Flash `HyperCharge_ModuleOnly_Build-08.zip` in KernelSU / Magisk and reboot.

---

## 🟢 Hardware & ROM Compatibility

* **Processors:** Qualcomm Snapdragon (SM8750 / Snapdragon 8 Elite, Snapdragon 8 Gen 1/2/3, Snapdragon 7+ Gen 2/3, Snapdragon 870/888, etc.)
* **Devices:** Xiaomi, Redmi, and POCO smartphones (Redmi K80, POCO F7, POCO F6, POCO F5, Xiaomi 14/15, etc.)
* **Operating Systems:**
  * **Xiaomi HyperOS (1.0 / 2.0)** & MIUI
  * **AOSP Custom ROMs** (LineageOS, EvolutionX, PixelOS, crDroid, Project Elixir, etc.) — completely removes the 30W AOSP thermal throttling limit!

---

## 👨‍💻 Credits & Attribution

* **Application Creator, UI & Governor Architecture:** [Rohit Sharma](https://github.com/ROHITSHARMA838888)
* **Base Module Attribution:** [@Demonx4y](https://github.com/Demonx4y) (Hyper-Charging-Plus)
