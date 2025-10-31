# SoundPay - Build & Installation Guide

## Quick Start

### 1. Build the Project
```bash
cd /Users/maheshmishra/AndroidStudioProjects/soundpay
./gradlew clean build
```

### 2. Install on Device/Emulator
```bash
# Install debug build
./gradlew installDebug

# Or build APK
./gradlew assembleDebug
# APK will be at: app/build/outputs/apk/debug/app-debug.apk
```

## First Time Setup

### Step 1: Grant Notification Access
1. Launch the app
2. You'll see a red status card saying "Service Inactive"
3. Tap the "Enable Access" button
4. In the system settings, find "SoundPay" and toggle it ON
5. Return to the app - the status should now be green "Service Active"

### Step 2: Configure Settings (Optional)
1. Tap the Settings icon (⚙️) in the top-right
2. Configure:
   - **Enable Announcements**: ON (default)
   - **Language**: English or Hindi
   - **Volume Boost**: ON (recommended)
   - **Auto Start on Boot**: ON (recommended)

### Step 3: Test the App
To test without making a real payment:
1. Use a UPI app on another device to send a small test payment
2. The notification should trigger an announcement
3. Check the "History" screen to see logged payments

## Project Structure Overview

```
soundpay/
├── app/
│   ├── src/main/
│   │   ├── java/com/example/soundpayapplication/
│   │   │   ├── data/              # Room database, DAO, entities
│   │   │   ├── service/           # Background services (Notification listener, TTS)
│   │   │   ├── receiver/          # Boot receiver
│   │   │   ├── util/              # Utilities (Parser, Preferences)
│   │   │   ├── viewmodel/         # ViewModels
│   │   │   ├── ui/
│   │   │   │   ├── screens/       # Compose screens (Home, History, Settings)
│   │   │   │   ├── navigation/    # Navigation routes
│   │   │   │   └── theme/         # Material 3 theme
│   │   │   └── MainActivity.kt
│   │   ├── res/
│   │   └── AndroidManifest.xml
│   └── build.gradle.kts
├── gradle/
│   └── libs.versions.toml         # Dependency versions
├── build.gradle.kts
├── settings.gradle.kts
└── README.md
```

## Key Components

### 1. PaymentNotificationListener (Service)
- Listens to all notifications
- Filters UPI app notifications
- Parses payment information
- Saves to database and triggers announcement

### 2. PaymentAnnouncementService (Foreground Service)
- Runs Text-to-Speech
- Manages volume boost
- Supports English and Hindi
- Runs in foreground to prevent killing

### 3. PaymentParser (Utility)
- Extracts amount using regex (₹123, Rs 123, etc.)
- Extracts sender name from notification text
- Handles various notification formats

### 4. Room Database
- Stores payment history locally
- Provides statistics (today, week, month)
- Supports deletion and cleanup

### 5. UI Screens
- **Home**: Dashboard with statistics and service status
- **History**: List of all payment announcements
- **Settings**: Configure app behavior

## Debugging

### Enable Verbose Logging
Check Android Logcat for tags:
- `PaymentNotificationListener` - Notification processing
- `PaymentAnnouncementService` - TTS announcements
- `PaymentParser` - Amount/sender extraction

### Common Issues

**Build fails with AAR metadata error:**
- Solution: Already fixed - compileSdk and targetSdk set to 36

**App doesn't announce:**
- Check notification access is granted
- Verify announcements are enabled in settings
- Check device volume is not zero
- Look for errors in Logcat

**Service stops:**
- Disable battery optimization for the app
- Enable auto-start in settings
- Check if OS is killing background services

## Testing Checklist

- [ ] App installs successfully
- [ ] Notification access can be granted
- [ ] Home screen shows service status
- [ ] Settings can be changed and persist
- [ ] Payment notification triggers announcement
- [ ] Payment appears in history
- [ ] Statistics update correctly
- [ ] App works when screen is locked
- [ ] App restarts after device reboot
- [ ] Volume boost works
- [ ] Language change works

## Supported UPI Apps
The app monitors notifications from:
- PhonePe (com.phonepe.app)
- Google Pay (com.google.android.apps.nbu.paisa.user)
- Paytm (net.one97.paytm)
- BHIM (in.org.npci.upiapp)
- Amazon Pay (in.amazon.mShop.android.shopping)
- BharatPe (com.bharatpe.merchant.flutter)
- Freecharge (com.freecharge.android)
- MobiKwik (com.mobikwik_new)
- WhatsApp (com.whatsapp)

## Release Build

To create a release build:

```bash
# Generate release APK (unsigned)
./gradlew assembleRelease

# Sign the APK (you'll need a keystore)
# Follow Android documentation for signing
```

## Next Steps

1. **Test thoroughly** with real UPI payments
2. **Improve parsing** based on actual notification formats
3. **Add more UPI apps** if needed
4. **Customize UI** to match brand requirements
5. **Add analytics** to track usage patterns

## Support

For issues or questions:
1. Check Logcat for errors
2. Review the README.md
3. Examine notification text format
4. Test with different UPI apps

---
**Happy Building! 🚀**

