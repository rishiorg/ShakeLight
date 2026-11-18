# Shake Light

Shake your phone twice, firmly, to toggle the flashlight, even with the screen off.
A foreground service listens to the accelerometer and calls
`CameraManager.setTorchMode` (no camera permission needed).

## Build the APK

**Option A: Android Studio**
1. Open this folder in Android Studio (Ladybug or newer) and let Gradle sync.
2. Build > Build APK(s), or plug in the phone (USB debugging on) and press Run.

**Option B: no PC tools, use GitHub**
1. Create a GitHub repo and push this folder to it.
2. Open the Actions tab > "Build APK" > wait ~3 min.
3. Download the `ShakeLight-debug-apk` artifact, unzip, install `app-debug.apk` on the phone.

## Tuning
- `Prefs.DEFAULT_THRESHOLD` and the in-app slider set how hard a jolt must be.
- `ShakeDetector.requiredJolts` (4 = two back-and-forth shakes) and `windowMs` set the pattern.
