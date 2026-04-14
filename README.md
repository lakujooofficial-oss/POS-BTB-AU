# POS-BTB-AU

Android POS application with Bluetooth ESC/POS thermal printer support.

## Features
- Product catalogue with quick add-to-cart
- Cart checkout and receipt generation
- Daily sales reports and printable reports
- Bluetooth thermal printer support for ESC/POS printers

## Build and run
1. Install Android Studio or the Android SDK.
2. Set the SDK location by creating a `local.properties` file with:

```properties
sdk.dir=/path/to/Android/Sdk
```

3. Open this folder in Android Studio, or build from the terminal:

```bash
./gradlew assembleDebug
```

4. Install the debug APK on a physical Android device.

> Pair your Bluetooth thermal printer first through Android system settings.

## Notes
- This sample app uses a simple in-memory product catalog and persistent daily sales stored in `SharedPreferences`.
- Printing uses classic Bluetooth RFCOMM with ESC/POS commands.
