# Image Compressor (Android)

A small native Android app that lets you select multiple images, convert them to JPEG, choose JPEG quality (10–100%), optionally resize the longest edge, and save compressed copies to `Pictures/ImageCompressor`. It does not modify originals and does not upload images.

## Build an APK
1. Install Android Studio (current stable) with Android SDK Platform 35.
2. Open this `ImageCompressor` folder in Android Studio.
3. Allow Gradle sync to complete.
4. Choose **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
5. Android Studio will show the generated APK location (usually `app/build/outputs/apk/debug/app-debug.apk`).

Minimum Android version: Android 6.0 (API 23). The project uses only Android platform APIs.

## Notes
- Selected images are read through Android's system document picker.
- Output is JPEG, so transparent areas in PNG images will be flattened.
- Quality and resize settings trade image size for visual detail.
