<div align="center">

# 🎥 Virexa Screen

### Professional Screen Recorder for Android

Modern, lightweight, and powerful screen recording application built with Kotlin and Jetpack Compose.

[![Android](https://img.shields.io/badge/Android-8.0%2B-green?style=for-the-badge&logo=android)](https://developer.android.com/)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.x-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-blue?style=for-the-badge)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-Proprietary-red?style=for-the-badge)]()

</div>

---

## 📱 Overview

**Virexa Screen** is a modern Android screen recording application designed to provide a smooth, professional, and user-friendly experience.

The application combines Android's MediaProjection API, Foreground Services, and a floating control system to allow users to record their screen while maintaining full control from anywhere on the device.

Designed for:

- 🎓 Students
- 🎥 Content creators
- 💼 Professionals
- 🛠 Technical support teams
- 📚 Educators

---

## ✨ Features

### 🎬 Screen Recording

- Start screen recording instantly
- Pause recording
- Resume recording
- Stop recording
- Foreground service support
- Real-time recording status
- Elapsed time tracking

---

### 🪟 Floating Control Bubble

Control recordings from any application.

Available actions:

- Open application
- Pause recording
- Resume recording
- Stop recording
- Start new recording
- View recording status
- View recording timer

---

### 📂 Recording Management

- Recording history
- File management
- Share recordings
- Delete recordings
- Quick access to saved videos

---

### 🎨 Modern UI

Built entirely with Jetpack Compose.

Features:

- Material Design 3
- Dynamic state management
- Smooth animations
- Modern navigation
- Responsive layouts

---

## 🏗 Architecture

The project follows a modern MVVM architecture.

```text
┌───────────────────────────┐
│        UI Layer           │
│      Jetpack Compose      │
└─────────────┬─────────────┘
              │
              ▼
┌───────────────────────────┐
│       ViewModels          │
│      State Management     │
└─────────────┬─────────────┘
              │
              ▼
┌───────────────────────────┐
│      Repository Layer     │
└─────────────┬─────────────┘
              │
              ▼
┌───────────────────────────┐
│       Data Sources        │
└─────────────┬─────────────┘
              │
              ▼
┌───────────────────────────┐
│ Android Framework APIs    │
└───────────────────────────┘
```

---

## 📂 Project Structure

```text
app/
│
├── data/
│   ├── model/
│   ├── repository/
│   └── datastore/
│
├── service/
│   ├── ScreenRecordService.kt
│   ├── FloatingBubbleService.kt
│   └── NotificationHelper.kt
│
├── ui/
│   ├── screens/
│   ├── components/
│   ├── theme/
│   └── navigation/
│
├── viewmodel/
│
└── MainActivity.kt
```

---

## 🧰 Tech Stack

### Language

- Kotlin

### UI

- Jetpack Compose
- Material Design 3

### Architecture

- MVVM
- Repository Pattern

### Async Processing

- Kotlin Coroutines
- StateFlow

### Storage

- DataStore

### Android APIs

- MediaProjection API
- Foreground Services
- WindowManager
- Notifications

---

## 📚 Libraries & Dependencies

### Jetpack Compose

Official UI Toolkit for Android.

https://developer.android.com/jetpack/compose

---

### Material Design 3

Modern Android design system.

https://m3.material.io/

---

### AndroidX Lifecycle

Lifecycle-aware components.

https://developer.android.com/jetpack/androidx/releases/lifecycle

---

### AndroidX Navigation

Navigation framework for Compose.

https://developer.android.com/jetpack/compose/navigation

---

### Kotlin Coroutines

Asynchronous programming.

https://github.com/Kotlin/kotlinx.coroutines

---

### Android DataStore

Modern replacement for SharedPreferences.

https://developer.android.com/topic/libraries/architecture/datastore

---

### MediaProjection API

Official Android screen capture framework.

https://developer.android.com/reference/android/media/projection/MediaProjection

---

## 🔐 Permissions

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE"/>

<uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>

<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW"/>
```

---

## 🚀 Getting Started

### Clone Repository

```bash
git clone https://github.com/yourusername/VirexaScreen.git
```

### Open Project

```bash
Android Studio Meerkat+
```

### Build

```bash
./gradlew assembleDebug
```

### Generate Release APK

```bash
./gradlew assembleRelease
```

---

## 🎯 Use Cases

### Education

- Online classes
- Tutorials
- Learning content

### Content Creation

- YouTube videos
- Shorts
- TikTok
- Instagram Reels

### Business

- Training sessions
- Presentations
- Documentation

### Technical Support

- Bug reporting
- Demonstrations
- Device troubleshooting

---

## 🛣 Roadmap

### Version 1.x

- [x] Screen recording
- [x] Floating controls
- [x] Recording history
- [x] Material 3 UI

### Version 2.x

- [ ] Internal audio recording
- [ ] Resolution settings
- [ ] FPS settings
- [ ] Bitrate settings

### Version 3.x

- [ ] Video editor
- [ ] Facecam overlay
- [ ] Cloud sync
- [ ] AI-powered summaries

---

## 📊 Performance Goals

| Feature | Target |
|----------|----------|
| Startup Time | < 2 seconds |
| RAM Usage | < 150 MB |
| Recording Stability | 99% |
| Crash-Free Sessions | 99.5% |

---

## 👨‍💻 Developer

### Pedro Avendaño

Founder and Developer of Virexa Screen.

Focused on building modern Android applications using scalable architectures, Jetpack Compose, and modern Android development practices.

### Contact

- GitHub: https://github.com/Gh0stDeveloper
- Email: ghostnexora@gmail.com

---

<div align="center">

### Virexa Screen

**Record. Control. Share.**

Built with ❤️ using Kotlin & Jetpack Compose

</div>