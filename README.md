# ZDplayer

ZDplayer is a fully custom-built Android application created by ZULUDEFENCE SYSTEMS for real-time FPV (First Person View) video streaming, recording, and snapshot capturing over USB serial connection.




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

