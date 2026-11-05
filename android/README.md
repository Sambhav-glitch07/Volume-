# Sambhav Glass Volume (Android TV)

Standalone Android TV volume overlay replicating the Apple-style frosted glass HUD for **TCL Android TV (Android 9 / API 28)**.

![Reference Image](public/assets/aistudio/1793893375822_4688efe7.jpg)

## 🌟 Key Highlights
- **Exact Reference Reproduction**: Translucent smoky dark glass capsule, curved liquid meniscus fill with specular blue-white reflection highlight, speaker icon, live volume number ("52"), and circular AirPlay-style audio output button.
- **Hardware Remote Interception**: Intercepts `KEYCODE_VOLUME_UP`, `KEYCODE_VOLUME_DOWN`, and `KEYCODE_VOLUME_MUTE` via `AccessibilityService` with `FLAG_REQUEST_FILTER_KEY_EVENTS`.
- **SystemUI Popup Silencer**: Adjusts media stream volume via `AudioManager` **without** `FLAG_SHOW_UI`, preventing the default TV manufacturer volume popup from appearing.
- **Pure Native Views & Canvas**: Built using pure Android Views, custom hardware-accelerated 2D Canvas rendering, and `ValueAnimator`—zero Jetpack Compose bloat, ensuring smooth 60fps performance on TV stick processors.
- **10-Foot TV Setup UI**: High-contrast, D-Pad focusable TV dashboard with active status detection, in-app live preview, and direct shortcut to Accessibility Settings.
- **Automated GitHub Actions Build**: Automatically produces `app-debug.apk` ready to sideload.

---

## 📁 Repository Structure
```
├── .github/
│   └── workflows/
│       └── build.yml
├── app/
│   ├── build.gradle
│   ├── proguard-rules.pro
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml
│           ├── java/in/sambhav/glassvolume/
│           │   ├── MainActivity.kt
│           │   ├── GlassVolumeService.kt
│           │   ├── ui/
│           │   │   └── GlassVolumeView.kt
│           │   └── util/
│           │       └── ServiceUtils.kt
│           └── res/
│               ├── drawable/
│               │   ├── banner.xml
│               │   ├── btn_tv_focus_selector.xml
│               │   ├── bg_glass_card.xml
│               │   ├── ic_audio_output.xml
│               │   ├── ic_launcher.xml
│               │   └── ic_speaker.xml
│               ├── layout/
│               │   ├── activity_main.xml
│               │   └── layout_volume_overlay.xml
│               ├── values/
│               │   ├── colors.xml
│               │   ├── strings.xml
│               │   └── styles.xml
│               └── xml/
│                   └── volume_service_config.xml
├── gradle/
│   └── wrapper/
│       └── gradle-wrapper.properties
├── build.gradle
├── gradle.properties
├── gradlew
├── settings.gradle
└── README.md
```

---

## 🚀 How to Build via GitHub Actions (Zero Local Setup)
1. Push this repository to GitHub.
2. Go to the **Actions** tab on your GitHub repository.
3. The **Build Sambhav Glass Volume APK** workflow triggers automatically.
4. Once completed (~2 minutes), click the run and download the `Sambhav-Glass-Volume-AndroidTV-Debug` artifact containing `app-debug.apk`.

---

## 📺 How to Sideload & Install on TCL Android TV

### Method 1: Direct USB Thumbdrive (Easiest)
1. Copy `app-debug.apk` to a FAT32/exFAT formatted USB drive.
2. Plug the USB into your TCL TV.
3. Open the TV File Manager / "Safety Guard" app and install the APK.
4. If prompted, allow "Install Unknown Apps" from File Manager.

### Method 2: "Send Files to TV" (Wirelessly via Android Phone)
1. Install **Send Files to TV** from Google Play Store on both your TV and Android phone.
2. Send `app-debug.apk` from phone to TV.
3. Use a TV file manager (e.g. FX File Explorer or TV File Commander) to click and install the received APK.

### Method 3: Wireless ADB
```bash
adb connect <YOUR_TV_IP_ADDRESS>:5555
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

## ⚙️ Enabling Sambhav Glass Volume on TCL Android TV
1. Launch **Sambhav Glass Volume** from your TV apps list or Leanback launcher.
2. Click **ENABLE GLASS VOLUME**.
3. TV settings will open:
   - Go to **Device Preferences** > **Accessibility** > **Downloaded Services**.
   - Select **Sambhav Glass Volume**.
   - Toggle **Enable** to **ON** and press **OK** on the permission prompt.
4. Press the **BACK** button on your remote to return to the app.
5. The status badge will now show **ACTIVE & INTERCEPTING (GREEN)**.
6. Press the **Vol +** or **Vol -** button on your TV remote to enjoy the custom Apple-style glass volume HUD!
