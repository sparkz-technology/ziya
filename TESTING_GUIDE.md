# Testing Guide for Enhanced Ziya Notification Service

## Quick Start Testing

### 1. Start the WebSocket Server
```bash
cd socket/
npm install
npm start
```
The server will start on `http://localhost:3000` with WebSocket endpoint `ws://localhost:3000`

### 2. Test the Enhanced Features

#### A. Dynamic WebSocket URL Configuration
1. **Launch the app** on your Android device/emulator
2. **Change the WebSocket URL**:
   - Default: `ws://192.168.1.9:3000`
   - For local testing: `ws://10.0.2.2:3000` (Android emulator)
   - For device testing: `ws://YOUR_COMPUTER_IP:3000`
3. **Tap "Save URL"** - should see confirmation toast
4. **Monitor debug log** - should show "WebSocket URL saved: [new_url]"

#### B. Connection Testing
1. **Tap "Test Connection"** button
2. **Watch debug log** for connection results:
   - Success: "Test connection successful!"
   - Failure: "Test connection failed: [error]"
3. **Verify status indicators** update accordingly

#### C. Service Management
1. **Service Status** should show "Running" in green
2. **WebSocket Status** should progress: "Connecting" → "Connected"
3. **Test service controls**:
   - "Stop Service" → Status changes to "Stopped"
   - "Start Service" → Service restarts and reconnects

#### D. Notification Testing
1. **Tap "Test Notification"** → Should see notification appear
2. **Send from server**:
   ```bash
   curl -X POST http://localhost:3000/notify \
     -H "Content-Type: application/json" \
     -d '{"message":"Hello from server!"}'
   ```
3. **Use web interface**: Visit `http://localhost:3000` and send messages

## Advanced Testing Scenarios

### 1. Background Operation Testing
```bash
# Test background notification reception
adb shell am start -S com.example.ziya/.MainActivity
adb shell input keyevent KEYCODE_HOME  # Send app to background
curl -X POST http://localhost:3000/notify -H "Content-Type: application/json" -d '{"message":"Background test"}'
# Should receive notification even when app is backgrounded
```

### 2. Network Interruption Testing
```bash
# Start app and connect to server
# Then stop server to test reconnection
npm stop  # Stop server
# Watch debug log show reconnection attempts
npm start # Restart server
# Watch automatic reconnection
```

### 3. URL Validation Testing
Test invalid URLs in the app:
- `http://example.com` (should reject - not WebSocket)
- `example.com:3000` (should reject - no protocol)
- Empty field (should reject)
- `ws://` (should reject - incomplete)

### 4. Permission Testing
```bash
# Test notification permissions
adb shell pm revoke com.example.ziya android.permission.POST_NOTIFICATIONS
# Launch app - should request permission
# Grant permission - should start service automatically
```

## Expected Behavior

### Connection States
1. **Disconnected** (Red) - No active connection
2. **Connecting** (Orange) - Attempting to connect
3. **Connected** (Green) - Active WebSocket connection
4. **Error** (Red) - Connection failed

### Debug Log Messages
```
[HH:MM:SS] MainActivity initialized
[HH:MM:SS] WebSocket URL saved: ws://10.0.2.2:3000
[HH:MM:SS] Starting notification service...
[HH:MM:SS] NotificationService created
[HH:MM:SS] Connecting to WebSocket: ws://10.0.2.2:3000
[HH:MM:SS] WebSocket connected successfully
[HH:MM:SS] Received message: Hello from server!
[HH:MM:SS] Notification shown: Hello from server!
```

### Notification Behavior
- **Foreground**: Notifications appear while app is visible
- **Background**: Notifications appear when app is minimized/closed
- **Test notifications**: Immediate local notification without server
- **Server notifications**: Triggered by WebSocket messages

## Troubleshooting

### Common Issues
1. **"Connection failed"**:
   - Check server is running
   - Verify IP address/port
   - Check firewall settings
   - For emulator, use `10.0.2.2` instead of `localhost`

2. **"No notifications appearing"**:
   - Check notification permissions
   - Verify notification channels are created
   - Check device "Do Not Disturb" settings

3. **"Service keeps stopping"**:
   - Check battery optimization settings
   - Verify foreground service permissions
   - Check device background app restrictions

### Debug Commands
```bash
# Check if service is running
adb shell ps | grep ziya

# Check notification permissions
adb shell dumpsys notification | grep ziya

# Monitor logs
adb logcat | grep ziya

# Check network connectivity
adb shell ping YOUR_SERVER_IP
```

## Server Testing Commands

### Send Individual Notifications
```bash
# Simple message
curl -X POST http://localhost:3000/notify \
  -H "Content-Type: application/json" \
  -d '{"message":"Test notification"}'

# Formatted message with emoji
curl -X POST http://localhost:3000/notify \
  -H "Content-Type: application/json" \
  -d '{"message":"🚀 Server notification at $(date)"}'
```

### Check Server Status
```bash
# Health check
curl http://localhost:3000/health

# Connected clients
curl http://localhost:3000/clients

# Automated test notification
curl http://localhost:3000/test
```

### Stress Testing
```bash
# Send multiple notifications
for i in {1..10}; do
  curl -X POST http://localhost:3000/notify \
    -H "Content-Type: application/json" \
    -d "{\"message\":\"Test message $i\"}"
  sleep 1
done
```

This testing guide ensures all new features work correctly across different scenarios and edge cases.