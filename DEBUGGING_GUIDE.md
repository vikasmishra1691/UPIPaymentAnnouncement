# SoundPay - Debugging Guide for Payment Detection

## Issue: Payment Not Detected

If you received a payment but the app didn't announce it or update the UI, follow these steps:

---

## Step 1: Check Notification Listener Permission ⚠️

**This is the MOST COMMON issue!**

### How to Enable:

1. Open your phone's **Settings**
2. Go to **Apps** or **Applications**
3. Find **SoundPay** in the app list
4. Tap on **Notifications** or **Special app access**
5. Look for **Notification access** or **Notification listener**
6. Enable the toggle for **SoundPay**

### Alternative Method:
1. Open SoundPay app
2. If service shows "Service Inactive" in red
3. Tap "Enable Permission" button
4. Toggle ON SoundPay in the system settings
5. Return to app

### To Verify:
- Open SoundPay
- Check the status card at the top
- It should show **"Service Active"** in GREEN
- If it shows **"Service Inactive"** in RED, permission is not granted

---

## Step 2: Check Logcat Output

Connect your phone to your computer and run:

```bash
# Clear previous logs and watch in real-time
adb logcat -c && adb logcat | grep -E "(PaymentNotificationListener|PaymentParser|PaymentAnnouncementService)"
```

### What to Look For:

#### When notification arrives, you should see:
```
D/PaymentNotificationListener: ========================================
D/PaymentNotificationListener: Notification received from: com.google.android.apps.nbu.paisa.user
D/PaymentNotificationListener: ✓ UPI app detected: com.google.android.apps.nbu.paisa.user
D/PaymentNotificationListener: --- Notification Details ---
D/PaymentNotificationListener: Package: com.google.android.apps.nbu.paisa.user
D/PaymentNotificationListener: Title: Payment received
D/PaymentNotificationListener: Text: You received ₹1.00
D/PaymentNotificationListener: Payment Detection:
D/PaymentNotificationListener:   - Has currency: true
D/PaymentNotificationListener:   - Has received keyword: true
D/PaymentNotificationListener:   - Has sent keyword: false
D/PaymentNotificationListener:   - Is UPI context: true
D/PaymentNotificationListener:   - Final result: PAYMENT RECEIVED ✓
D/PaymentNotificationListener: ✓ Parsed payment successfully: ₹1.00 from [sender]
D/PaymentNotificationListener: ✓ Payment saved to database
D/PaymentNotificationListener: ✓ Triggering voice announcement
```

#### If you see NOTHING:
- **Problem**: Notification Listener permission not enabled
- **Solution**: Go to Step 1 and enable permission

#### If you see "✗ Ignoring non-UPI app":
- **Problem**: Notification is not from a recognized UPI app
- **Solution**: Check if the package name is in our list

#### If you see "✗ NOT payment received":
- **Problem**: Notification text doesn't match our detection patterns
- **Solution**: Share the notification text with us to improve detection

---

## Step 3: Test with ADB Command

You can manually trigger a test notification:

```bash
# Send a test notification (works on some devices)
adb shell cmd notification post -S bigtext -t "Payment received" \
  "Tag" "You received ₹100 from Test User via Google Pay"
```

---

## Step 4: Common Issues & Solutions

### Issue: "Service Inactive" shows in red

**Cause**: Notification Listener permission not granted  
**Solution**: 
1. Tap "Enable Permission" button in app
2. Enable SoundPay in system settings
3. Return to app and refresh

### Issue: Notification appears but no announcement

**Possible Causes**:
1. **Announcements disabled in Settings**
   - Go to Settings → Toggle ON "Enable Announcements"

2. **TTS not initialized**
   - Check logcat for TTS errors
   - Install Google TTS or Samsung TTS from Play Store

3. **App battery optimization**
   - Go to Settings → Battery → App power management
   - Set SoundPay to "No restrictions"

### Issue: Database not updating (UI shows 0)

**Possible Causes**:
1. **Database error**
   - Check logcat: `adb logcat | grep "Error saving to database"`
   
2. **UI not refreshing**
   - Close and reopen the app
   - Check if data persists

3. **Parsing failed**
   - Check logcat for "Could not parse payment info"
   - Notification text format might be unsupported

---

## Step 5: Manual Testing Steps

### Test 1: Check Service Status
1. Open SoundPay app
2. Look at top status card
3. Should show "Service Active" in GREEN

### Test 2: Check Permissions
1. Go to phone Settings → Apps → SoundPay
2. Check Notifications → Notification access
3. Should be ENABLED

### Test 3: Test with Real Payment
1. Ask a friend to send you ₹1 via Google Pay or PhonePe
2. Watch the app immediately after payment
3. You should hear announcement within 2-3 seconds
4. UI should update with new total

### Test 4: Check Logs
1. Connect phone to computer
2. Run: `adb logcat -c && adb logcat | grep PaymentNotificationListener`
3. Receive a payment
4. Watch logs in terminal

---

## Step 6: Getting the Exact Notification Text

If the app is not detecting your payment notification, we need to see the exact text:

### Method 1: Using Logcat (Recommended)
```bash
# This will show ALL notification details
adb logcat | grep -E "(Notification received|Title:|Text:|BigText:)"
```

### Method 2: Manual Note
1. When you receive payment, screenshot the notification
2. Note down:
   - Exact notification title
   - Exact notification text
   - Which app (Google Pay, PhonePe, etc.)

---

## Quick Checklist ✓

Before asking for help, verify:

- [ ] Notification Listener permission is ENABLED in system settings
- [ ] SoundPay shows "Service Active" (green) on home screen
- [ ] Announcements are ENABLED in app settings
- [ ] You received payment (not sent payment)
- [ ] Payment is from a supported UPI app (GPay, PhonePe, Paytm, etc.)
- [ ] Battery optimization is DISABLED for SoundPay
- [ ] You have checked logcat output
- [ ] TTS (Text-to-Speech) is installed on device

---

## Supported UPI Apps

The app detects notifications from these 18 UPI apps:

✓ PhonePe  
✓ Google Pay  
✓ Paytm  
✓ BHIM UPI  
✓ Amazon Pay  
✓ BharatPe  
✓ Freecharge  
✓ MobiKwik  
✓ WhatsApp Pay  
✓ PayPal  
✓ CRED  
✓ Airtel Payments  
✓ iMobile Pay  
✓ Axis Mobile  
✓ YONO SBI  
✓ HDFC Bank  
✓ Federal Bank  
✓ PNB One  

If your UPI app is not in this list, the notification will be ignored.

---

## Advanced Debugging Commands

### Get all logs from SoundPay:
```bash
adb logcat | grep -E "(com.example.soundpayapplication|PaymentNotification|PaymentAnnouncement)"
```

### Check if service is running:
```bash
adb shell dumpsys notification_listener
```

### Check recent notifications:
```bash
adb shell dumpsys notification | grep -A 20 "google.android.apps.nbu.paisa.user"
```

### Force stop and restart app:
```bash
adb shell am force-stop com.example.soundpayapplication
adb shell am start -n com.example.soundpayapplication/.MainActivity
```

---

## What to Share When Asking for Help

Please provide:

1. **Logcat output** (from the moment you received payment)
2. **Screenshot** of the payment notification
3. **App screenshot** showing service status (active/inactive)
4. **Which UPI app** you received payment from
5. **Exact notification text** (title and message)

With this information, we can quickly identify and fix the issue!

---

## Expected Behavior

### When Everything Works:

1. ✓ You receive payment on Google Pay/PhonePe
2. ✓ Google Pay shows notification
3. ✓ SoundPay detects notification within 100ms
4. ✓ Voice announcement plays: "Payment received of ₹X from [name]"
5. ✓ Home screen updates immediately:
   - Today's total increases by ₹X
   - Today's count increases by 1
   - This Week total increases
   - This Month total increases
6. ✓ Transaction appears in History screen
7. ✓ All happens within 2-3 seconds

If ANY of these steps fail, follow the debugging steps above!

---

**Last Updated**: November 3, 2025  
**App Version**: 1.0

