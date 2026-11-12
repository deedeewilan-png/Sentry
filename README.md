# Sentry

Android CCTV viewer and controller. Kotlin, Jetpack Compose, Media3 RTSP, ONVIF PTZ.

## Build the APK on GitHub

1. Create a new empty repository on github.com.
2. Upload the contents of this folder so that `.github/workflows/build.yml` is at the repo root.
3. Open the Actions tab, pick "Build APK", press Run workflow.
4. When it turns green, open the run and download the `sentry-debug-apk` artifact.
5. Unzip it and install `app-debug.apk` on your phone.
