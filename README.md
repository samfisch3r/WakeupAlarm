# Wakeup Alarm

A minimalist, high-reliability alarm clock app built with **Kotlin** and **Jetpack Compose**. Designed to be a bloat-free alternative to mainstream alarm apps while maintaining the essential features needed for a consistent wakeup routine.

## 🚀 Features

- **Reliability First**:
  - Automatically reschedules alarms after device reboots.
  - Proactive checks for Battery Optimization to prevent the system from killing alarms.
  - Guides users to grant "Exact Alarm" permissions for precise timing.
- **Customizable Alarms**:
  - Set multiple alarms with specific hours and minutes.
  - Repeat alarms on specific weekdays.
  - Toggle alarms on/off with a single tap.
- **Smart Snooze**:
  - Set your preferred snooze duration.
  - Limit the number of snoozes allowed—once reached, you *must* dismiss the alarm.
- **Waking Experience**:
  - **Gradual Volume**: Set a target volume and the duration over which it should slowly increase.
  - **Vibration**: Toggle vibration for every alarm.
  - **Lock Screen Support**: Alarm trigger screen shows over the lock screen with large, easy-to-hit buttons.
- **Music Selection**: Pick any audio file from your device to wake up to.
- **Modern Tech Stack**: Fully leverages the `java.time` API for high-precision alarm scheduling and locale-aware formatting.

## 🛠 Tech Stack

- **UI**: Jetpack Compose with Material 3.
- **Database**: Room for persistent alarm storage.
- **Architecture**: MVVM with StateFlow and ViewModel.
- **Service**: Android Foreground Service for reliable media playback.
- **Scheduling**: `AlarmManager` with `setExactAndAllowWhileIdle` and modern `java.time` APIs.
- **Min SDK**: 33 (Android 13).

## 📦 Installation & Setup

1. **Clone the project**:
   ```bash
   git clone https://github.com/your-repo/wakeup-alarm.git
   ```
2. **Open in Android Studio**: Use Ladybug or newer.
3. **Build & Run**: Ensure you are targeting a device with API 33 (Android 13) or higher.

## ⚠️ Important Configuration

To ensure your alarms are 100% reliable:
1. **Disable Battery Optimization**: When prompted by the app, go to settings and set Wakeup Alarm to "Don't Optimize" or "Unrestricted".
2. **Allow Exact Alarms**: Ensure the permission is granted in the system settings (the app will guide you there).
3. **Enable Notifications**: Necessary for the alarm to trigger the full-screen wake-up UI.

## 📄 License

This project is licensed under the Apache License 2.0 - see the LICENSE file for details.
