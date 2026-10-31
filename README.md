# ⚡ HyperCharge Control Hub

[![Developer: Rohit Sharma](https://img.shields.io/badge/Developed%20By-Rohit%20Sharma-blue?style=for-the-badge&logo=android)](https://github.com/ROHITSHARMA838888)
[![Built For: Hyper-Charging-Plus](https://img.shields.io/badge/Built%20To%20Control-Hyper--Charging--Plus-purple?style=for-the-badge&logo=github)](https://github.com/Demonx4y/Hyper-Charging-Plus)
[![Hardware: Xiaomi & POCO Snapdragon](https://img.shields.io/badge/Hardware-Xiaomi%20%26%20POCO%20Snapdragon-orange?style=for-the-badge)](https://github.com/ROHITSHARMA838888/rohits-ultra-fast-charging-mode)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-green.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)

**HyperCharge Control Hub** is an Android application developed and built from scratch by **Rohit Sharma** to serve as the official manual controller and live hardware telemetry dashboard for the **Hyper-Charging-Plus** KernelSU module.

---

## 📢 Fresh Build Note & Module Attribution

> ⚡ **Fresh Application Build:**  
> This application is **NOT a rebuild**. It was designed, engineered, and programmed from scratch by **Rohit Sharma**.  
> The original project by **[@Demonx4y](https://github.com/Demonx4y)** was a standalone KernelSU background script without any manual application interface.  
> Rohit Sharma created this dedicated companion application to give users instant 1-click mode switching, smooth animations, and real-time charging telemetry.

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
