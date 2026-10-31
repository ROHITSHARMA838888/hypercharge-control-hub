# ⚡ HyperCharge Control Hub

[![Developer: Rohit Sharma](https://img.shields.io/badge/Developed%20By-Rohit%20Sharma-blue?style=for-the-badge&logo=android)](https://github.com/ROHITSHARMA838888)
[![Root: Required (KernelSU / Magisk / APatch)](https://img.shields.io/badge/Root-Required%20(KernelSU%20%2F%20Magisk%20%2F%20APatch)-red?style=for-the-badge&logo=android)](https://github.com/ROHITSHARMA838888/hypercharge-control-hub)
[![Built For: Hyper-Charging-Plus](https://img.shields.io/badge/Built%20To%20Control-Hyper--Charging--Plus-purple?style=for-the-badge&logo=github)](https://github.com/Demonx4y/Hyper-Charging-Plus)
[![Hardware: Xiaomi & POCO Snapdragon](https://img.shields.io/badge/Hardware-Xiaomi%20%26%20POCO%20Snapdragon-orange?style=for-the-badge)](https://github.com/ROHITSHARMA838888/rohits-ultra-fast-charging-mode)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-green.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)

**HyperCharge Control Hub** is an Android application developed and built from scratch by **Rohit Sharma** to serve as the official manual controller and live hardware telemetry dashboard for the **Hyper-Charging-Plus** KernelSU module.

> ⚠️ **IMPORTANT PREREQUISITE: ROOT PERMISSION REQUIRED**  
> **HyperCharge Control Hub strictly requires Root Access (`su`)** via **KernelSU**, **Magisk (v20.4+)**, or **APatch**.  
> The app operates at the low-level kernel interface, reading real-time telemetry from `/sys/class/power_supply/` and writing configuration profiles to `/data/adb/modules/`. Make sure to tap **Grant** when the Superuser request dialog appears on first launch. On unrooted devices, the app cannot modify hardware charging limits or apply modes.

> 📢 **Fresh Build Note & Module Attribution:**  
> This application is **NOT a rebuild**. It was designed, engineered, and programmed from scratch by **Rohit Sharma**.  
> The original project by **[@Demonx4y](https://github.com/Demonx4y)** was a standalone KernelSU background script without any manual application interface.  
> Rohit Sharma created this dedicated companion application to give users instant 1-click mode switching, smooth animations, and real-time charging telemetry.

---

## 🎯 Mode Selection Guide (Based on Phone Wattage)

To avoid charging handshake crashes (such as the slow-charging bug on 18W/33W devices) and prevent excessive heat, select the mode that matches your phone's hardware wattage:

| Device Wattage Tier | Example Devices | Recommended Mode | Current Target | Description & Instructions |
| :--- | :--- | :--- | :--- | :--- |
| 🚀 **67W / 90W / 120W** | POCO F-Series (F4/F5/F6/F7), Redmi K-Series, Xiaomi Flagships | ⚡ **Ultra Turbo Mode** | `6.0A` (Max) | Delivers peak turbo wattage. **Peltier phone cooler recommended** for sustained 0–80% turbo speed without thermal stepping. |
| ⚖️ **33W / 45W** | Redmi Note 10/11/12/13 Pro, POCO X-Series | ⚖️ **Balanced Mode** | `4.5A` | Optimal daily fast charging. Perfectly matches the 33W–45W hardware current envelope without heating. |
| 🛡️ **18W / 22.5W** | Redmi 9T, Redmi 9/10/12/13C, POCO M-Series | 🛡️ **Eco Mode** | `2.5A` | **Crucial for budget phones!** Prevents Qualcomm PM6115 driver overload and stops the phone from dropping into slow 500mA charging. |
| 🎮 **All Devices (Gaming)** | Any Xiaomi / POCO phone during gaming | 🎮 **Gaming Bypass Mode** | `0.5A` (Idle) | Powers the phone during gaming without pumping excess heat into the battery. |
| 🔄 **All Devices (Default)**| Any phone with dynamic daily needs | 🔄 **Auto Governor** | `Dynamic` | Automatically scales between 2.0A and 6.0A across a multi-tier thermal ladder. |

---

## 💡 Solved Issues (Compared to Upstream Hyper-Charging-Plus)

1. **Fixed "Slow Charging" on 18W Devices (Issue #2):**
   * *Problem:* Upstream scripts forced a flat 6A request on all devices, causing budget PMICs (like Qualcomm PM6115 on Redmi 9T) to reject the handshake and fall back to slow 500mA trickle charging.
   * *Solution:* HyperCharge Control Hub lets budget device users select **Eco Mode (2.5A)** or **Balanced Mode (4.5A)**, ensuring clean negotiation at the device's actual maximum speed.
2. **Fixed 45°C Overheating Bug (Issue #3):**
   * *Problem:* Aggressive loops on uncalibrated kernels occasionally pushed current past 45°C.
   * *Solution:* Our governor enforces a **42°C Hard Thermal Ceiling** that immediately cuts current to 2.0A whenever battery temperatures exceed safe thresholds.

---

## 📱 Hardware & Device Compatibility

* **🟢 Xiaomi & POCO Snapdragon Devices — 100% Supersonic Working:**
  * Universal compatibility for **ALL Xiaomi, Redmi, and POCO devices running Qualcomm Snapdragon processors** with Fast Charging support (e.g. 33W, 67W, 90W, 120W).
  * Direct PMIC and HVDCP integration eliminates artificial AOSP thermal caps (such as the standard 30W drop).
* **🟡 Xiaomi & POCO MediaTek (Dimensity) Devices — Partial / Safe Fallback:**
  * Supported on kernels where standard power supply nodes are exposed.
  * **Zero-Crash Guarantee:** Includes safe node verification. If a node is missing, execution is skipped without causing system crashes or reboots.

---

## ✨ Features

* **⚡ Animated Splash Screen:** Glowing charge bolt animation with developer attribution.
* **🎛 Manual Mode Toggles (Clean & Concise):**
  * ⚡ **Ultra Turbo Mode** (6.0A Peak Speed — ideal with phone coolers)
  * ⚖️ **Balanced Mode** (4.5A Fast Charge — daily optimal use)
  * 🛡️ **Eco Mode** (2.5A Cool Charge — battery longevity)
  * 🎮 **Gaming Bypass Mode** (0.5A Idle Charge — stops heat buildup during gaming)
  * 🔄 **Auto Governor** (Dynamic multi-step thermal ladder)
* **📊 Live Hardware Telemetry:**
  * Real-time Battery Percentage
  * Battery Temperature (°C) with color-coded safety indicators
  * Current Flow (mA) & Charging Voltage (V)
* **🔄 Built-In In-App OTA Updater:**
  * Queries GitHub Releases API directly for 1-click updates.

---

## 📥 Downloads & Releases

Download both the Root Module and the Controller App from [GitHub Releases](https://github.com/ROHITSHARMA838888/rohits-ultra-fast-charging-mode/releases/latest).

---

## 👨‍💻 Credits & Upstream Attribution

* **Companion App Creator & Developer:** [Rohit Sharma](https://github.com/ROHITSHARMA838888)
* **Original KernelSU Module Base:** [@Demonx4y](https://github.com/Demonx4y) (Hyper-Charging-Plus)
