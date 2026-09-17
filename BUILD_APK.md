# Build the RS KICKBOX test APK

This repository includes `.github/workflows/build-preview-apk.yml`.

The workflow installs JDK 17, Android API 36, Build Tools 36.0.0 and Gradle, then runs `:app:assembleDebug` and uploads `app-debug.apk` as the artifact **RS-KICKBOX-preview-apk**.
