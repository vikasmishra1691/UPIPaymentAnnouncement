# SoundPay - UPI Payment Speaker App

## Overview
SoundPay is an Android application designed for small merchants, vendors, and shop owners to automatically announce incoming UPI payments aloud using the phone's speaker. The app detects payment notifications from UPI apps and plays a customizable voice alert announcing the received amount and sender's name.

## Features

### Core Features
- **Notification Listener**: Detects incoming UPI payment notifications from popular apps (PhonePe, Google Pay, Paytm, BHIM, Amazon Pay, BharatPe, Freecharge, MobiKwik, WhatsApp)
- **Smart Parsing**: Extracts amount and sender name from notification text using regex patterns
- **Voice Announcement**: Announces payment details using Text-to-Speech (TTS)
- **Payment History**: Keeps a record of all payment announcements with time, app name, and amount
- **Dashboard**: View today's, this week's, and this month's payment statistics
- **Offline Functionality**: Works without internet once the app is installed
- **Auto Start on Boot**: Automatically resumes listening service when phone restarts
- **Lock-Screen Functionality**: App continues working when phone is locked
- **Volume Control**: Ensures announcements are audible by automatically boosting volume

### Settings
- **Enable/Disable Announcements**: Toggle payment announcements on/off
- **Language Selection**: Choose between English and Hindi
- **Volume Boost**: Automatically increase volume for announcements
- **Auto Start**: Start service when device boots

## Technical Stack
- **Frontend**: Jetpack Compose (Material 3)
- **Backend**: Room Database, SharedPreferences
- **Language**: Kotlin
- **Architecture**: MVVM
- **Notification Handling**: NotificationListenerService
- **Voice**: Android TextToSpeech API
- **Background Execution**: Foreground Service

## System Requirements
- **Minimum Android SDK**: 26 (Android 8.0 Oreo)
- **Target SDK**: 36
- **Compile SDK**: 36
- **RAM**: 2 GB minimum

## Permissions Required
- Notification Access (BIND_NOTIFICATION_LISTENER_SERVICE)
- Boot Receiver (RECEIVE_BOOT_COMPLETED)
- Foreground Service
- Post Notifications
- Wake Lock
- Modify Audio Settings

## Setup Instructions

### 1. Clone and Build
```bash
git clone <repository-url>
cd soundpay
./gradlew build
```

### 2. Install on Device
```bash
./gradlew installDebug
```

### 3. Grant Notification Access
1. Open the app
2. Tap "Enable Access" on the home screen
3. Find "SoundPay" in the list and enable notification access
4. Return to the app

### 4. Configure Settings
1. Go to Settings from the home screen
2. Enable/disable announcements as needed
3. Choose your preferred language
4. Enable volume boost for louder announcements
5. Enable auto-start to run on boot

## How It Works

1. **User installs the app** and grants Notification Access Permission
2. **App listens** for notifications from known UPI apps using NotificationListenerService
3. **When a payment notification is detected**, the app:
   - Extracts amount and sender name via regex patterns
   - Saves the payment to local database
   - Passes details to Text-to-Speech engine
   - Announces: "Payment received of ₹150 from Rohan"
4. **Payment is logged** in the history for later reference
5. **Works in background** even when phone is locked

## Supported UPI Apps
- PhonePe
- Google Pay
- Paytm
- BHIM
- Amazon Pay
- BharatPe
- Freecharge
- MobiKwik
- WhatsApp

## Project Structure
```
app/
├── data/                           # Data layer
│   ├── PaymentEntity.kt           # Room entity
│   ├── PaymentDao.kt              # Database access object
│   ├── PaymentDatabase.kt         # Room database
│   └── PaymentRepository.kt       # Repository pattern
├── service/                        # Background services
│   ├── PaymentNotificationListener.kt  # Listens to notifications
│   └── PaymentAnnouncementService.kt   # TTS announcement service
├── receiver/                       # Broadcast receivers
│   └── BootReceiver.kt            # Auto-start on boot
├── util/                          # Utilities
│   ├── PaymentParser.kt           # Parse notification text
│   └── PreferenceManager.kt       # SharedPreferences wrapper
├── viewmodel/                     # ViewModels
│   └── PaymentViewModel.kt       # Main ViewModel
├── ui/                            # UI layer
│   ├── navigation/
│   │   └── Screen.kt              # Navigation routes
│   ├── screens/
│   │   ├── HomeScreen.kt          # Dashboard
│   │   ├── HistoryScreen.kt       # Payment history
│   │   └── SettingsScreen.kt      # Settings
│   └── theme/                     # Material theme
└── MainActivity.kt                # Entry point
```

## Key Performance Indicators (KPIs)
- **Average time from notification to announcement**: < 1 second
- **False positive detection rate**: < 5%
- **Background uptime**: 95%+
- **Memory usage**: < 50MB

## Privacy and Security
- ✅ No direct access to user's bank or UPI credentials
- ✅ Only reads notification text - no sensitive data stored externally
- ✅ All logs stored locally and deletable by user
- ✅ No internet connection required for operation
- ✅ No data shared with third parties

## Future Enhancements
- [ ] Bluetooth speaker support
- [ ] Cloud sync for analytics (optional)
- [ ] Payment reversal detection
- [ ] Custom alert sounds
- [ ] Multiple language support (regional languages)
- [ ] Export payment history to CSV/Excel
- [ ] Widget for quick stats

## Troubleshooting

### Announcements Not Working
1. Check if notification access is enabled
2. Verify announcements are enabled in settings
3. Check phone volume is not muted
4. Test with a UPI app payment

### Service Stops After Some Time
1. Enable auto-start in settings
2. Disable battery optimization for SoundPay
3. Check if the app has been force-stopped

### Cannot Hear Announcements
1. Enable volume boost in settings
2. Increase media volume manually
3. Check TTS engine is installed (Settings > Language & Input > Text-to-Speech)

## Contributing
Contributions are welcome! Please follow these steps:
1. Fork the repository
2. Create a feature branch
3. Commit your changes
4. Push to the branch
5. Create a Pull Request

## License
This project is licensed under the MIT License.

## Contact
For issues and feature requests, please create an issue on GitHub.

---
**Made with ❤️ for small merchants and vendors**

