# Ziya - Enhanced Notification Service

An Android application that provides real-time push notifications via WebSocket connections with a developer-friendly configuration interface.

## Features Implemented

### ✅ Dynamic WebSocket URL Configuration
- **Configurable WebSocket endpoint**: No longer hardcoded to a specific IP
- **Persistent storage**: URL settings saved using SharedPreferences
- **Runtime reconfiguration**: Change WebSocket URL without restarting the app
- **URL validation**: Ensures URLs start with `ws://` or `wss://`

### ✅ Enhanced Developer UI
- **Modern Material Design interface** with card-based layout
- **WebSocket URL input field** with validation
- **Real-time connection status indicators**
- **Service control buttons** (Start/Stop/Test)
- **Live debug log** with timestamps and auto-scroll
- **Test notification functionality**

### ✅ Improved Notification System
- **Robust foreground service** with proper lifecycle management
- **Enhanced notification channels** with appropriate priorities
- **Background and foreground operation** support
- **Automatic reconnection logic** with exponential backoff
- **Connection status broadcasting** to UI

### ✅ Enhanced WebSocket Management
- **Intelligent reconnection strategy** with configurable retry limits
- **Connection testing functionality** 
- **Better error handling and logging**
- **Proper cleanup on service destruction**
- **Status reporting** for connection states

## Technical Improvements

### Notification Fixes
1. **Proper permission handling** for Android 13+ (POST_NOTIFICATIONS)
2. **Enhanced notification channels** with correct importance levels
3. **Foreground service** with data sync service type
4. **Wake lock and network state permissions** for better background operation

### WebSocket Enhancements
1. **Configurable timeout settings** (15s connect, 30s read/write)
2. **Retry on connection failure** enabled
3. **Exponential backoff reconnection** (2s to 30s max delay)
4. **Maximum retry attempts** (5 attempts before giving up)
5. **Connection testing** without affecting main connection

### UI/UX Improvements
1. **Real-time status updates** via broadcast receivers
2. **Live debug logging** with 50-line history limit
3. **Color-coded status indicators** (green/orange/red)
4. **Responsive layout** with scroll view for smaller screens
5. **Material Design components** for modern appearance

## Configuration

### Default Settings
- **Default WebSocket URL**: `ws://192.168.1.9:3000`
- **Reconnection attempts**: 5 maximum
- **Initial reconnection delay**: 2 seconds
- **Maximum reconnection delay**: 30 seconds

### Permissions Required
- `FOREGROUND_SERVICE` - For background service operation
- `POST_NOTIFICATIONS` - For showing notifications (Android 13+)
- `INTERNET` - For WebSocket connections
- `ACCESS_NETWORK_STATE` - For network state monitoring
- `WAKE_LOCK` - For reliable background operation
- `FOREGROUND_SERVICE_DATA_SYNC` - For data synchronization service type

## Usage

### Setting Up WebSocket URL
1. Open the Ziya app
2. Enter your WebSocket server URL in the text field
3. Tap "Save URL" to persist the configuration
4. Use "Test Connection" to verify connectivity

### Managing the Service
1. **Start Service**: Begins the foreground notification service
2. **Stop Service**: Cleanly shuts down the service
3. **Test Notification**: Sends a test notification to verify functionality

### Monitoring Status
- **Service Status**: Shows if the background service is running
- **WebSocket Status**: Shows connection state (Connected/Connecting/Disconnected/Error)
- **Debug Log**: Real-time logging of all service activities

## Server Integration

The app connects to a WebSocket server that should:
1. Accept WebSocket connections
2. Send text messages that will be displayed as notifications
3. Handle connection/disconnection gracefully

### Example Server Endpoint
The included Node.js server (`socket/server.js`) provides:
- WebSocket server on port 3000
- HTTP endpoint `/notify` for sending messages
- Broadcasting to all connected clients

## Build Requirements

- Android SDK API 26+ (Android 8.0)
- Target SDK API 34
- Gradle 8.0+
- Android Gradle Plugin 7.4.2+
- OkHttp 4.12.0 for WebSocket support

## Architecture

### Service Design
- **Foreground Service**: Ensures reliable background operation
- **Broadcast Communication**: Real-time UI updates
- **SharedPreferences**: Persistent configuration storage
- **Handler-based Threading**: UI updates and reconnection scheduling

### Error Handling
- **Connection failure recovery** with exponential backoff
- **Graceful service shutdown** with proper cleanup
- **User feedback** for configuration errors
- **Debug logging** for troubleshooting

## Troubleshooting

### Common Issues
1. **Notifications not appearing**: Check notification permissions in Android settings
2. **Connection failures**: Verify WebSocket URL format and server availability
3. **Service stops**: Check battery optimization settings for the app
4. **Background limitations**: Ensure the app is not restricted by Android's background app limits

### Debug Information
Use the built-in debug log to monitor:
- Connection attempts and results
- Message reception
- Service lifecycle events
- Error conditions and recovery attempts