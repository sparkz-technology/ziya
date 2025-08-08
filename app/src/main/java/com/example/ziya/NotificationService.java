package com.example.ziya;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
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

public class NotificationService extends Service {

    private static final String CHANNEL_ID = "BackgroundNotificationChannel";
    private static final int NOTIFICATION_ID = 1;
    private static final String WEBSOCKET_URL = "ws://192.168.1.9:3000"; // Replace with your backend IP

    private OkHttpClient client;
    private WebSocket webSocket;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, getForegroundNotification());
        client = new OkHttpClient();
        connectWebSocket();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
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

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Background Notifications",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private Notification getForegroundNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Background Notification Service")
                .setContentText("Listening for notifications")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .build();
    }

    private void showNotification(String message) {
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Custom Push Notification")
                .setContentText(message)
                .setSmallIcon(android.R.drawable.ic_dialog_alert)
                .setAutoCancel(true)
                .build();

        NotificationManager manager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        manager.notify((int) System.currentTimeMillis(), notification);
    }

    private void connectWebSocket() {
        Request request = new Request.Builder().url(WEBSOCKET_URL).build();
        SocketListener listener = new SocketListener();
        webSocket = client.newWebSocket(request, listener);
    }

    // --- WebSocketListener using OkHttp ---
    private class SocketListener extends WebSocketListener {
        @Override
        public void onOpen(@NonNull WebSocket webSocket, @NonNull Response response) {
            super.onOpen(webSocket, response);
            System.out.println("OkHttp WebSocket: Connected!");
        }

        @Override
        public void onMessage(@NonNull WebSocket webSocket, @NonNull String text) {
            super.onMessage(webSocket, text);
            // When a message is received, show a notification
            showNotification(text);
        }

        @Override
        public void onClosing(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            super.onClosing(webSocket, code, reason);
            webSocket.close(1000, null);
            System.out.println("OkHttp WebSocket: Closing: " + code + " / " + reason);
        }

        @Override
        public void onClosed(@NonNull WebSocket webSocket, int code, @NonNull String reason) {
            super.onClosed(webSocket, code, reason);
            System.out.println("OkHttp WebSocket: Closed: " + code + " / " + reason);
            // Schedule a reconnect attempt
            reconnect();
        }

        @Override
        public void onFailure(@NonNull WebSocket webSocket, @NonNull Throwable t, @Nullable Response response) {
            super.onFailure(webSocket, t, response);
            System.out.println("OkHttp WebSocket: Error: " + t.getMessage());
            t.printStackTrace();
            // Schedule a reconnect attempt on failure
            reconnect();
        }

        private void reconnect() {
            // Reconnect after 5 seconds
            handler.postDelayed(() -> connectWebSocket(), 5000);
        }
    }
}