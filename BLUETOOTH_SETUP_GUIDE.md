# 🚀 Quick Setup Guide - Bluetooth Feature

## Build & Test the Bluetooth Feature

### Step 1: Build the Project
```bash
cd /Users/maheshmishra/AndroidStudioProjects/soundpay
./gradlew clean build
```

### Step 2: Install on Device
```bash
./gradlew installDebug
```

Or build APK:
```bash
./gradlew assembleDebug
# APK located at: app/build/outputs/apk/debug/app-debug.apk
```

### Step 3: Test the Feature

#### A. Initial Setup
1. Open SoundPay app
2. Scroll to "Audio Output" section on Home screen
3. You'll see **"Bluetooth Speaker"** card

#### B. Enable Bluetooth Announcements
1. Toggle the switch to **ON**
2. If permissions not granted:
   - Tap "Grant Permission"
   - Allow Bluetooth permissions
3. If Bluetooth is off:
   - Tap "Enable Bluetooth"
   - Enable in system settings

#### C. Select Bluetooth Device
1. Tap **"Select Device"** button
2. In dialog, choose **"Paired"** tab
3. Select your Bluetooth speaker/headphones from list
4. Device name will show in card

**OR scan for new devices:**
1. In dialog, choose **"Available"** tab
2. Tap **"Scan"** button
3. Wait for devices to appear
4. Select your device

#### D. Test Announcement
**Method 1: Real Payment**
- Send yourself money via PhonePe/Google Pay
- Announcement should play through Bluetooth speaker! 🔊

**Method 2: Test Notification** (for testing)
- Use another app to trigger a test UPI notification
- Or send yourself a small payment

---

## 📋 Expected Behavior

### When Bluetooth is ENABLED:
- ✅ Payment announcement plays through **Bluetooth speaker**
- ✅ Volume boost is disabled (uses Bluetooth volume)
- ✅ Audio focus is requested
- ✅ TTS routes to Bluetooth automatically

### When Bluetooth is DISABLED:
- ✅ Payment announcement plays through **phone speaker**
- ✅ Volume boost works (if enabled in settings)
- ✅ Normal behavior

### Audio Isolation:
- ❌ YouTube videos → Phone speaker (NOT Bluetooth)
- ❌ Music apps → Phone speaker (NOT Bluetooth)
- ❌ Ringtones → Phone speaker (NOT Bluetooth)
- ✅ Payment announcements → Bluetooth speaker ONLY

---

## 🐛 Troubleshooting

### Issue: "Bluetooth not supported"
**Solution**: Device doesn't have Bluetooth hardware. Feature won't work.

### Issue: "Bluetooth permission required"
**Solution**: 
1. Tap "Grant Permission"
2. Allow all Bluetooth permissions
3. If denied, go to Settings > Apps > SoundPay > Permissions > Enable Bluetooth

### Issue: "Bluetooth is turned off"
**Solution**: 
1. Tap "Enable Bluetooth" in app
2. Or manually enable in Android settings

### Issue: "No device selected"
**Solution**:
1. Tap "Select Device"
2. Make sure Bluetooth device is paired with phone
3. Select device from list

### Issue: Announcement plays through phone, not Bluetooth
**Check:**
1. ✅ Bluetooth toggle is ON in app
2. ✅ Bluetooth device is selected
3. ✅ Bluetooth device is connected to phone
4. ✅ Bluetooth device is actually on and in range

### Issue: No devices shown in "Paired" tab
**Solution**:
1. Exit app
2. Go to Android Settings > Bluetooth
3. Pair your Bluetooth speaker manually
4. Return to app and reload

### Issue: Scan doesn't find devices
**Solution**:
1. Make sure Bluetooth device is in pairing mode
2. Make sure device is close to phone
3. Try "Stop" and "Scan" again
4. Check Bluetooth permissions are granted

---

## 🧪 Testing Checklist

### Basic Functionality:
- [ ] Toggle Bluetooth ON → Shows device selection options
- [ ] Toggle Bluetooth OFF → Hides device options
- [ ] Can see paired devices in list
- [ ] Can select a device
- [ ] Selected device name shows in card
- [ ] Can change device
- [ ] Can disconnect device

### Permission Tests:
- [ ] First time: requests permissions properly
- [ ] Denied permissions: shows rationale
- [ ] Granted permissions: enables feature

### Bluetooth State:
- [ ] Bluetooth off: prompts to enable
- [ ] Bluetooth on: works normally
- [ ] Device unpaired: shows in available devices

### Announcement Tests:
- [ ] With Bluetooth ON: announcement through speaker ✅
- [ ] With Bluetooth OFF: announcement through phone ✅
- [ ] Hindi language: works with Bluetooth ✅
- [ ] English language: works with Bluetooth ✅
- [ ] Low latency: < 1 second ✅

### Audio Isolation:
- [ ] Play YouTube video → phone speaker (not Bluetooth) ✅
- [ ] Play music → phone speaker (not Bluetooth) ✅
- [ ] Receive call → phone speaker (not Bluetooth) ✅
- [ ] Payment announcement → Bluetooth speaker ✅

---

## 📸 Expected UI

### Bluetooth Card - Disabled State
```
┌────────────────────────────────────┐
│ 🔵 Bluetooth Speaker        [OFF] │
│                                    │
│ Enable to play announcements      │
│ through Bluetooth speaker         │
└────────────────────────────────────┘
```

### Bluetooth Card - Enabled, No Device
```
┌────────────────────────────────────┐
│ 🔵 Bluetooth Speaker        [ON]  │
│ ────────────────────────────────── │
│ 🔍 No device selected              │
│                                    │
│ [    Select Device    ]           │
│                                    │
│ Announcements will play through   │
│ the selected Bluetooth speaker    │
└────────────────────────────────────┘
```

### Bluetooth Card - Device Connected
```
┌────────────────────────────────────┐
│ 🔵 Bluetooth Speaker        [ON]  │
│ ────────────────────────────────── │
│ 🔗 JBL Flip 5                      │
│                                    │
│ [  Change  ]    [ Disconnect ]    │
│                                    │
│ Announcements will play through   │
│ the selected Bluetooth speaker    │
└────────────────────────────────────┘
```

---

## 🎯 Success Criteria

Your implementation is successful when:

1. ✅ UI shows Bluetooth card on home screen
2. ✅ Can toggle Bluetooth ON/OFF
3. ✅ Can select Bluetooth devices
4. ✅ Payment announcements play through Bluetooth
5. ✅ Latency is under 1 second
6. ✅ Only announcements route to Bluetooth (not media/ringtones)
7. ✅ Permissions handled gracefully
8. ✅ Works with both Hindi and English
9. ✅ Settings persist across app restarts
10. ✅ No crashes or errors

---

## 🎉 Done!

If all tests pass, your Bluetooth speaker feature is working perfectly! 

Users can now enjoy payment announcements through their Bluetooth speakers with:
- 🎯 Low latency (< 1 second)
- 🔊 Clear audio quality
- 🎨 Beautiful UI
- 🛡️ Proper permission handling
- 🧹 Clean architecture

**Ready for production!** 🚀

