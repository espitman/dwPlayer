# dwPlayer - Android TV Cinema Hub & Web Companion 🎬

**dwPlayer** is a modern, high-performance media player and cinema hub built specifically for **Android TV**, complete with a companion web dashboard for remote control, background downloads, series playlists, and local network streaming (SMB).

---

## ✨ Features

- **📺 Native Android TV UI**: Designed exclusively for D-pad navigation with Compose TV and Jetpack Compose.
- **🌐 Web Companion Dashboard**: Embedded Ktor HTTP server (`http://<tv-ip>:8200`) providing:
  - Link submission for direct downloads
  - Live download progress, pause/resume, and cancellation
  - Playlist & series manager with episode reordering (Up/Down)
  - Remote media control (Play All, Play Episode)
  - Video archive file manager with disk storage analytics
- **🎬 Advanced Media Player**:
  - Powered by AndroidX Media3 / ExoPlayer
  - D-pad optimized OSD with seek bar focus (+/- 10s seeking)
  - Audio track & Subtitle track selector
  - Aspect ratio switcher (Fit, Fill, Zoom, 16:9, 4:3)
  - Playback speed control (0.5x - 2.0x)
  - Continuous playback (Auto-Next episode in playlists)
  - 30-second countdown banner before the next episode
- **📂 Video Archive**:
  - Automatically indexes downloaded and local media
  - Complete disk management with in-app deletion and free space monitoring
- **🌐 Local Network Streaming (SMB)**:
  - Connect to PC, Mac, or NAS shared folders via SMB
  - Stream videos smoothly over Wi-Fi without downloading
- **⚡ Resilient Download Engine**:
  - Multi-threaded segmented download manager with automatic retry & resume

---

## 🛠️ Tech Stack

- **Platform**: Android TV (API 26+ / Android 8.0+)
- **Language**: Kotlin 2.0
- **UI Framework**: Jetpack Compose for TV / Material 3
- **Media Engine**: AndroidX Media3 (ExoPlayer)
- **Local Database**: Room 2.6 (SQLite)
- **Embedded Web Server**: Ktor 2.3 (Netty engine)
- **Dependency Injection**: Dagger Hilt
- **Network & SMB**: OkHttp 4, SMBJ

---

## 🚀 Getting Started

### Building from Source

1. Clone the repository:
   ```bash
   git clone https://github.com/espitman/dwPlayer.git
   cd dwPlayer
   ```

2. Build debug APK:
   ```bash
   ./gradlew assembleDebug
   ```

3. Build signed release APK:
   ```bash
   ./gradlew assembleRelease
   ```

---

## 📄 License
MIT License
