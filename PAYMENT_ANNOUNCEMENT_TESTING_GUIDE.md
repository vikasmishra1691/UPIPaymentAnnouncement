# Payment Announcement Service - Testing & Troubleshooting Guide

## Overview
This guide helps you verify that the NotificationListenerService is working correctly and troubleshoot payment announcement issues.

## ✅ Pre-Flight Checklist

### 1. Notification Listener Permission
The app **MUST** have notification listener permission enabled:

**How to Enable:**
1. Go to: **Settings → Apps → Special app access → Notification access**
2. Find **SoundPay** in the list
3. Toggle it **ON**
4. Accept the permission prompt

**Verify in App:**
- Open SoundPay app
- Check the status card at the top of the Home Screen
- Should show: **"Service Active - Monitoring payments"** (Green)
- If red: Click "Enable" button to open settings

### 2. Announcement Settings
Ensure announcements are enabled in app settings:
1. Open SoundPay
2. Tap Settings (gear icon)
3. Verify "Enable Announcements" is **ON**
4. Choose your preferred language (English/Hindi)

### 3. Phone Audio Settings
- Media volume should be at audible level
- Phone is not in silent/vibrate mode (or test with volume up)
- No Bluetooth devices connected (announcements use phone speaker only now)

## 🔍 Testing Payment Announcements

### Method 1: Test with Real UPI App
1. **Open logcat** to monitor logs:
   ```bash
   adb logcat | grep -E "(PaymentNotificationListener|PaymentAnnouncementService)"
   ```

2. **Simulate a payment** using any UPI app:
   - Have someone send you ₹1 via PhonePe, Google Pay, or Paytm
   - OR send yourself money between your own accounts

3. **Expected Behavior:**
   - Toast notification: "Payment notification detected!"
   - Toast notification: "Payment: ₹1 from [Name]"
   - TTS announcement through phone speaker
   - Payment appears in app's Transaction History

### Method 2: Send Test Notification
You can create a test notification that mimics a UPI payment:

**Using ADB:**
```bash
# Test PhonePe notification
adb shell am broadcast -a com.phonepe.app.PAYMENT_RECEIVED \
  --es title "Payment Received" \
  --es text "You received ₹100 from John Doe via UPI"

# Note: This requires a custom test harness - use Method 1 for real testing
```

## 📊 Log Monitoring

### What to Look For in Logs

#### 1. Service Connected (on app start)
```
D/PaymentNotificationListener: Notification Listener Connected
```
✅ **Good**: Service is active and listening
❌ **Missing**: Permission not granted or service not registered

#### 2. Notification Received
```
D/PaymentNotificationListener: ========================================
D/PaymentNotificationListener: Notification received from: com.phonepe.app
D/PaymentNotificationListener: ✓ UPI app detected: com.phonepe.app
```
✅ **Good**: Notification detected from UPI app
❌ **Missing**: Permission issue or notification not from supported UPI app

#### 3. Payment Detection
```
D/PaymentNotificationListener: --- Notification Details ---
D/PaymentNotificationListener: Package: com.phonepe.app
D/PaymentNotificationListener: Title: Payment Received
D/PaymentNotificationListener: Text: You received ₹100 from John
D/PaymentNotificationListener: ✓ Detected as PAYMENT RECEIVED notification
```
✅ **Good**: Correctly identified as payment notification
❌ **Missing**: Notification doesn't match payment patterns

#### 4. Payment Parsing
```
D/PaymentNotificationListener: ✓ Parsed payment successfully: ₹100 from John Doe
```
✅ **Good**: Amount and sender extracted
❌ **Missing**: Parsing failed - check PaymentParser logic

#### 5. Database Save
```
D/PaymentNotificationListener: ✓ Payment saved to database
D/PaymentNotificationListener:   Amount: ₹100
D/PaymentNotificationListener:   Sender: John Doe
D/PaymentNotificationListener:   App: PhonePe
```
✅ **Good**: Payment stored in database
❌ **Missing**: Database error

#### 6. Announcement Service Start
```
D/PaymentAnnouncementService: ========================================
D/PaymentAnnouncementService: ★ PAYMENT ANNOUNCEMENT SERVICE CREATED ★
D/PaymentAnnouncementService: ========================================
D/PaymentAnnouncementService: ★ ANNOUNCEMENT SERVICE STARTED ★
D/PaymentAnnouncementService: Amount: ₹100
D/PaymentAnnouncementService: Sender: John Doe
```
✅ **Good**: Announcement service triggered
❌ **Missing**: Service not starting - check manifest registration

#### 7. TTS Initialization
```
D/PaymentAnnouncementService: → onInit() called with status: 0
D/PaymentAnnouncementService: ✓ TTS initialization SUCCESS
D/PaymentAnnouncementService: ✓ Language set successfully
D/PaymentAnnouncementService: ✓✓✓ TTS is now READY ✓✓✓
```
✅ **Good**: TTS engine ready
❌ **Missing**: TTS initialization failed (status != 0)

#### 8. Announcement
```
D/PaymentAnnouncementService: ★★★ ANNOUNCING: Payment received of ₹100 from John Doe ★★★
D/PaymentAnnouncementService: → TTS speak() returned: 0
D/PaymentAnnouncementService: ✓ TTS speak() call successful
D/PaymentAnnouncementService: ★ TTS STARTED speaking
D/PaymentAnnouncementService: ★ TTS COMPLETED speaking
```
✅ **Good**: Full announcement cycle
❌ **Missing**: TTS speak failed or audio routing issue

## 🐛 Common Issues & Solutions

### Issue 1: "Service Inactive" on Home Screen
**Symptoms:**
- Red status card showing "Service Inactive"
- "Enable notification access" message

**Solution:**
1. Tap "Enable" button in app
2. Find SoundPay in notification access settings
3. Toggle ON
4. Return to app - should now show green "Service Active"

**Verify in Logs:**
```bash
adb logcat | grep "Notification Listener Connected"
```

---

### Issue 2: Notifications Detected but No Announcement
**Symptoms:**
- Logs show "Notification received from: com.phonepe.app"
- No TTS announcement heard

**Possible Causes:**
1. **Announcements disabled in settings**
   - Check Settings → Enable Announcements is ON
   - Log will show: "Announcements disabled in settings"

2. **TTS not initialized**
   - Check logs for: "TTS initialization FAILED"
   - Solution: Ensure Google TTS or system TTS is installed

3. **Audio routing issue**
   - Check media volume is up
   - Check phone not muted
   - Log will show: "TTS speak() returned: -1" (ERROR)

4. **Not detected as payment received**
   - Log shows: "NOT payment received ✗"
   - Notification might be for payment SENT, not received
   - Check notification text contains "received", "credited", etc.

---

### Issue 3: Wrong Language Announced
**Symptoms:**
- Announcement plays in wrong language
- Garbled audio

**Solution:**
1. Go to Settings in app
2. Change Language to English or Hindi
3. Test again
4. If still wrong: Check system TTS language data is downloaded
   - Go to: Settings → System → Languages → Text-to-speech output
   - Download language data if missing

---

### Issue 4: No Notifications Detected at All
**Symptoms:**
- No logs from PaymentNotificationListener
- Service appears active but no activity

**Solution:**
1. **Verify service is running:**
   ```bash
   adb shell dumpsys notification_listener
   ```
   Should list: `com.example.soundpayapplication/.service.PaymentNotificationListener`

2. **Restart the service:**
   - Force stop the app
   - Clear app from recent apps
   - Relaunch app
   - Check logs for "Notification Listener Connected"

3. **Re-grant permission:**
   - Disable notification access for SoundPay
   - Enable it again
   - Restart app

4. **Test with non-UPI notification:**
   - Send yourself any notification
   - Check if logs show: "Notification received from: [package]"
   - If YES but UPI notifications don't work: UPI app package name might not be in supported list

---

### Issue 5: Announcement Cuts Off or Doesn't Complete
**Symptoms:**
- TTS starts but stops mid-sentence
- Log shows "TTS STARTED" but not "TTS COMPLETED"

**Solution:**
1. **Service stopping too early:**
   - Check logs for premature stopSelf() calls
   - Ensure UtteranceProgressListener is properly attached

2. **Audio focus lost:**
   - Another app might be stealing audio focus
   - Check no music/video playing during test

3. **TTS engine issue:**
   - Clear TTS cache: Settings → Apps → Google Text-to-Speech → Clear data
   - Restart phone
   - Test again

---

### Issue 6: Database Not Updating / UI Not Refreshing
**Symptoms:**
- Payment announced but doesn't show in history
- Today's earnings don't update

**Solution:**
1. **Check database save logs:**
   ```
   D/PaymentNotificationListener: ✓ Payment saved to database
   ```

2. **Verify ViewModel is observing database:**
   - HomeScreen should auto-refresh when resumed
   - Pull down to refresh (if implemented)
   - Navigate away and back to Home Screen

3. **Check UI state collection:**
   - Logs should show: "UI State Updated: Today Total: ₹XXX"
   - If not updating: ViewModel might not be collecting Flow correctly

---

## 🔧 Advanced Debugging

### Enable Verbose Logging
To see ALL notifications (not just UPI):

Edit `PaymentNotificationListener.kt`:
```kotlin
// Change this line:
Log.v(TAG, "✗ Ignoring non-UPI app: $packageName")

// To this:
Log.d(TAG, "✗ Ignoring non-UPI app: $packageName")
```

Then set log level to DEBUG:
```bash
adb logcat *:S PaymentNotificationListener:D PaymentAnnouncementService:D
```

### Check Running Services
```bash
# List all services
adb shell dumpsys activity services | grep -A 5 PaymentNotificationListener

# Check foreground service
adb shell dumpsys activity services | grep -A 10 PaymentAnnouncementService
```

### Inspect Database
```bash
# Pull database file
adb pull /data/data/com.example.soundpayapplication/databases/payment_database.db

# View with sqlite3
sqlite3 payment_database.db "SELECT * FROM payments ORDER BY timestamp DESC LIMIT 10;"
```

### Test TTS Directly
```bash
# Test if TTS works at all
adb shell am start -a android.speech.tts.engine.INSTALL_TTS_DATA
```

## 📱 Supported UPI Apps

The app listens for notifications from:
- ✅ PhonePe
- ✅ Google Pay
- ✅ Paytm
- ✅ BHIM UPI
- ✅ Amazon Pay
- ✅ BharatPe
- ✅ Freecharge
- ✅ MobiKwik
- ✅ WhatsApp Pay
- ✅ PayPal
- ✅ CRED
- ✅ Airtel Payments Bank
- ✅ iMobile Pay (ICICI)
- ✅ Axis Mobile
- ✅ YONO SBI
- ✅ HDFC Bank
- ✅ Federal Bank
- ✅ PNB One

**Not listed?** Add the package name to `upiApps` list in `PaymentNotificationListener.kt`

## ✅ Complete Test Checklist

Run through this checklist:

- [ ] Notification listener permission enabled (check in Settings)
- [ ] Green "Service Active" status in app
- [ ] Announcements enabled in app settings
- [ ] Media volume audible
- [ ] Phone not in silent mode
- [ ] Logs show "Notification Listener Connected"
- [ ] Test payment sent via UPI app
- [ ] Logs show notification received
- [ ] Logs show payment detected and parsed
- [ ] Toast notifications appear
- [ ] TTS announcement plays
- [ ] Payment appears in Transaction History
- [ ] Today's total updates on Home Screen

## 🎯 Expected Complete Flow

When everything works correctly:

1. **Payment arrives** → PhonePe/GPay notification
2. **Service detects** → "Notification received from: com.phonepe.app"
3. **Payment identified** → "✓ Detected as PAYMENT RECEIVED notification"
4. **Parsed successfully** → "✓ Parsed payment successfully: ₹100 from John"
5. **Toast shown** → "Payment: ₹100 from John Doe"
6. **Database saved** → "✓ Payment saved to database"
7. **Service started** → "★ ANNOUNCEMENT SERVICE STARTED ★"
8. **TTS initialized** → "✓✓✓ TTS is now READY ✓✓✓"
9. **Announcement plays** → "★★★ ANNOUNCING: Payment received..."
10. **Audio heard** → Clear TTS voice through phone speaker
11. **TTS completes** → "★ TTS COMPLETED speaking"
12. **UI updates** → Home screen shows new payment and updated totals

## 📞 Still Not Working?

If you've tried everything and it's still not working:

1. **Capture full logs:**
   ```bash
   adb logcat -c  # clear logs
   # Send test payment
   adb logcat > soundpay_debug.log
   # Send CTRL+C after 30 seconds
   ```

2. **Check for errors:**
   ```bash
   grep -E "(ERROR|Exception|FAILED)" soundpay_debug.log
   ```

3. **Verify service registration:**
   - Check `AndroidManifest.xml` has `PaymentNotificationListener` service
   - Check `android:permission="android.permission.BIND_NOTIFICATION_LISTENER_SERVICE"`
   - Check intent-filter is present

4. **Clean rebuild:**
   ```bash
   ./gradlew clean
   ./gradlew assembleDebug
   adb install -r app/build/outputs/apk/debug/app-debug.apk
   ```

5. **Factory test:**
   - Uninstall app completely
   - Restart phone
   - Reinstall app
   - Grant all permissions
   - Test again

---

**Last Updated:** November 22, 2025
**Status:** Enhanced with comprehensive logging and toast notifications

