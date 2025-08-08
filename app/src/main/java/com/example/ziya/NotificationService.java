package com.example.ziya;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

import java.util.concurrent.TimeUnit;

public class NotificationService extends Service {

    private static final String CHANNEL_ID = "BackgroundNotificationChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String PREFS_NAME = "ZiyaPrefs";
    private static final String KEY_WEBSOCKET_URL = "websocket_url";
    private static final String DEFAULT_WEBSOCKET_URL = "ws://192.168.1.9:3000";

    // Actions for communication with MainActivity
    public static final String ACTION_UPDATE_URL = "com.example.ziya.UPDATE_URL";
    public static final String ACTION_TEST_CONNECTION = "com.example.ziya.TEST_CONNECTION";
    public static final String ACTION_SEND_TEST_NOTIFICATION = "com.example.ziya.SEND_TEST_NOTIFICATION";
    public static final String ACTION_SERVICE_STATUS = "com.example.ziya.SERVICE_STATUS";
    public static final String ACTION_WEBSOCKET_STATUS = "com.example.ziya.WEBSOCKET_STATUS";
    public static final String ACTION_DEBUG_LOG = "com.example.ziya.DEBUG_LOG";

    private OkHttpClient client;
    private WebSocket webSocket;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private SharedPreferences sharedPreferences;
    private String currentWebSocketUrl;
    private boolean isConnecting = false;
    private int reconnectAttempts = 0;
    private static final int MAX_RECONNECT_ATTEMPTS = 5;
    private static final long INITIAL_RECONNECT_DELAY = 2000; // 2 seconds
    private static final long MAX_RECONNECT_DELAY = 30000; // 30 seconds

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        initializeClient();
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        currentWebSocketUrl = sharedPreferences.getString(KEY_WEBSOCKET_URL, DEFAULT_WEBSOCKET_URL);
        
        sendServiceStatus("Running");
        sendDebugLog("NotificationService created");
        
        startForeground(NOTIFICATION_ID, getForegroundNotification());
        connectWebSocket();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_UPDATE_URL:
                    String newUrl = intent.getStringExtra("websocket_url");
                    updateWebSocketUrl(newUrl);
                    break;
                case ACTION_TEST_CONNECTION:
                    String testUrl = intent.getStringExtra("websocket_url");
                    testConnection(testUrl);
                    break;
                case ACTION_SEND_TEST_NOTIFICATION:
                    sendTestNotification();
                    break;
            }
        }
        return START_STICKY; // Restart service if killed
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sendServiceStatus("Stopped");
        sendDebugLog("NotificationService destroyed");
        
        // Cleanly close the WebSocket and shut down the client
        if (webSocket != null) {
            webSocket.close(1000, "Service Destroyed");
        }
        if (client != null) {
            client.dispatcher().executorService().shutdown();
        }
        // Remove any pending reconnect attempts
        handler.removeCallbacksAndMessages(null);
    }

    private void initializeClient() {
        client = new OkHttpClient.Builder()
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription("Channel for background notification service");
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification getForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Ziya Notification Service")
                .setContentText("Listening for notifications on: " + getShortUrl(currentWebSocketUrl))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setOngoing(true)
                .build();
    }

    private String getShortUrl(String url) {
        if (url.length() > 30) {
            return url.substring(0, 27) + "...";
        }
        return url;
    }

    private void updateForegroundNotification() {
        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify(NOTIFICATION_ID, getForegroundNotification());
    }

    private void showNotification(String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Push Notification")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), notification);
        sendDebugLog("Notification shown: " + message);
    }

    private void sendTestNotification() {
        showNotification("This is a test notification from Ziya!");
        sendDebugLog("Test notification sent");
    }

    private void updateWebSocketUrl(String newUrl) {
        if (newUrl != null && !newUrl.equals(currentWebSocketUrl)) {
            sendDebugLog("Updating WebSocket URL from " + currentWebSocketUrl + " to " + newUrl);
            currentWebSocketUrl = newUrl;
            
            // Close existing connection
            if (webSocket != null) {
                webSocket.close(1000, "URL Updated");
            }
            
            // Update foreground notification
            updateForegroundNotification();
            
            // Connect to new URL
            handler.postDelayed(this::connectWebSocket, 1000);
        }
    }

    private void testConnection(String testUrl) {
        if (testUrl == null || testUrl.trim().isEmpty()) {
            sendDebugLog("Test connection failed: No URL provided");
            return;
        }

        sendDebugLog("Testing connection to: " + testUrl);
        
        Request request = new Request.Builder().url(testUrl).build();
        TestWebSocketListener testListener = new TestWebSocketListener();
        WebSocket testWebSocket = client.newWebSocket(request, testListener);
        
        // Close test connection after 5 seconds
        handler.postDelayed(() -> {
            testWebSocket.close(1000, "Test Complete");
        }, 5000);
    }

    private void connectWebSocket() {
        if (isConnecting) {
            sendDebugLog("Already connecting, skipping...");
            return;
        }

        isConnecting = true;
        sendWebSocketStatus("Connecting");
        sendDebugLog("Connecting to WebSocket: " + currentWebSocketUrl);

        try {
            Request request = new Request.Builder().url(currentWebSocketUrl).build();
            SocketListener listener = new SocketListener();
            webSocket = client.newWebSocket(request, listener);
        } catch (Exception e) {
            sendDebugLog("Failed to create WebSocket connection: " + e.getMessage());
            sendWebSocketStatus("Error");
            isConnecting = false;
            scheduleReconnect();
        }
    }

    private void scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            sendDebugLog("Max reconnect attempts reached. Stopping reconnection.");
            sendWebSocketStatus("Failed");
            return;
        }

        long delay = Math.min(INITIAL_RECONNECT_DELAY * (1L << reconnectAttempts), MAX_RECONNECT_DELAY);
        reconnectAttempts++;
        
        sendDebugLog("Scheduling reconnect attempt " + reconnectAttempts + " in " + (delay/1000) + " seconds");
        handler.postDelayed(this::connectWebSocket, delay);
    }

    private void resetReconnectAttempts() {
        reconnectAttempts = 0;
    }

    // Broadcast methods for communication with MainActivity
    private void sendServiceStatus(String status) {
        Intent intent = new Intent(ACTION_SERVICE_STATUS);
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    private void sendWebSocketStatus(String status) {
        Intent intent = new Intent(ACTION_WEBSOCKET_STATUS);
        intent.putExtra("status", status);
        sendBroadcast(intent);
    }

    private void sendDebugLog(String message) {
        Intent intent = new Intent(ACTION_DEBUG_LOG);
        intent.putExtra("message", message);
        sendBroadcast(intent);
    }

    // Main WebSocket listener
    private class SocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            super.onOpen(webSocket, response);
            isConnecting = false;
            resetReconnectAttempts();
            sendWebSocketStatus("Connected");
            sendDebugLog("WebSocket connected successfully");
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            super.onMessage(webSocket, text);
            sendDebugLog("Received message: " + text);
            showNotification(text);
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            super.onClosing(webSocket, code, reason);
            webSocket.close(1000, null);
            sendDebugLog("WebSocket closing: " + code + " / " + reason);
            sendWebSocketStatus("Disconnecting");
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            super.onClosed(webSocket, code, reason);
            isConnecting = false;
            sendDebugLog("WebSocket closed: " + code + " / " + reason);
            sendWebSocketStatus("Disconnected");
            
            // Schedule reconnect if not intentionally closed
            if (code != 1000) {
                scheduleReconnect();
            }
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            isConnecting = false;
            sendDebugLog("WebSocket error: " + t.getMessage());
            sendWebSocketStatus("Error");
            scheduleReconnect();
        }
    }

    // Test WebSocket listener for connection testing
    private class TestWebSocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            super.onOpen(webSocket, response);
            sendDebugLog("Test connection successful!");
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            sendDebugLog("Test connection failed: " + t.getMessage());
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            super.onClosed(webSocket, code, reason);
            sendDebugLog("Test connection closed");
        }
    }
}