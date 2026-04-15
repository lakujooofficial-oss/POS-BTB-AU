# POS-BTB-AU

Android POS application with Bluetooth ESC/POS thermal printer support.

## Supabase Setup
1. Go to your Supabase project dashboard at https://supabase.com/dashboard
2. Open the SQL Editor
3. Copy and paste the contents of `supabase_setup.sql` and run it
4. This will create the required tables: `products`, `promotions`, and `sales`

## Features
- Product catalogue with add/remove functionality
- Promotion/discount management
- Cart checkout with discount application
- Daily and weekly sales reports
- Printable receipts and reports via Bluetooth ESC/POS thermal printer
- Supabase data synchronization for products, promotions, and sales

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
- Supabase sync is supported for products, promotions, and sales if corresponding tables exist in the Supabase project.
