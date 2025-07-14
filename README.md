# ZDplayer

ZDplayer is a fully custom-built Android application created from scratch for real-time FPV (First Person View) video streaming, recording, and snapshot capturing over USB serial connection. It is designed for drone-based ground station use and optimized for landscape and floating mode.

---

## 🏗️ Built from Scratch

This app was developed **from the ground up** without relying on any pre-built Android templates or clone repositories. The entire architecture, UI, and recording pipeline were custom-designed by the team at **Zulu Defence**.

### 📁 Package Structure

Located under:  
`app/src/main/java
-Android
-com/
  - `shenyaocn` 
  - `zdplayer`


## 🧰 Tech Stack

- **Language:** Java
- **UI Toolkit:** Android XML layouts
- **USB Access:** `UsbSerialConnection`, `USBMonitor`
- **Video Decoding:** H.264 via `FPVVideoClient`


---

## 📦 How to Build

1. Open the project in **Android Studio**
2. Go to **Build > Build APK(s)**
3. Output file:  
   `app/build/outputs/apk/debug/app-debug.apk`

---

## 🏁 Deployment Notes

- Ensure USB serial video device is connected
- Grant permissions when prompted
- Tap **Start Recording** or **Snapshot** to save media into:
  - `Downloads/ZDPlayer/video/`
  - `Downloads/ZDPlayer/snap/`

