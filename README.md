# Green Tycoon (Native Android Game)

A clean, modern Android tycoon simulation game inspired by the mechanics of classic plant management games, built with Kotlin, Jetpack Compose, Procedural Canvas, and Android KeyStore.

## Automated Cloud APK Build via GitHub Actions

This repository includes a pre-configured GitHub Actions workflow (`.github/workflows/build-apk.yml`) that automatically compiles and packages the `.apk` file in the cloud. You **do not** need Android Studio installed on your computer.

### How to Get Your APK:

1. **Create a GitHub Repository:**
   - Go to [github.com/new](https://github.com/new) and create a new repository (e.g. `green-tycoon`).
2. **Push this project to your repository:**
   Run these commands from this directory:
   ```bash
   git remote add origin https://github.com/YOUR_USERNAME/green-tycoon.git
   git push -u origin main
   ```
3. **Download the APK:**
   - Go to your repository on GitHub.
   - Click on the **Actions** tab at the top.
   - You will see the **"Compile & Package APK"** workflow running automatically.
   - Once it finishes (usually 1-2 minutes), click on the completed run.
   - Under **Artifacts**, click **`GreenTycoon-Debug-APK`** to download your ready-to-install `app-debug.apk`!
4. **Install on Phone:**
   - Transfer the `.apk` to your Android phone or test it directly on [Appetize.io](https://appetize.io/).
