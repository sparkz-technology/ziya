package com.example.ziya;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.method.ScrollingMovementMethod;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.textfield.TextInputEditText;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "ZiyaPrefs";
    private static final String KEY_WEBSOCKET_URL = "websocket_url";
    private static final String DEFAULT_WEBSOCKET_URL = "ws://192.168.1.9:3000";
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;

    private TextInputEditText etWebSocketUrl;
    private TextView tvServiceStatus, tvWebSocketStatus, tvDebugLog;
    private Button btnSaveUrl, btnTestConnection, btnStartService, btnStopService;
    private Button btnClearLog, btnSendTestNotification;

    private SharedPreferences sharedPreferences;
    private Handler mainHandler;
    private StringBuilder debugLogBuilder;

    private BroadcastReceiver serviceStatusReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action != null) {
                switch (action) {
                    case NotificationService.ACTION_SERVICE_STATUS:
                        String serviceStatus = intent.getStringExtra("status");
                        updateServiceStatus(serviceStatus);
                        break;
                    case NotificationService.ACTION_WEBSOCKET_STATUS:
                        String wsStatus = intent.getStringExtra("status");
                        updateWebSocketStatus(wsStatus);
                        break;
                    case NotificationService.ACTION_DEBUG_LOG:
                        String logMessage = intent.getStringExtra("message");
                        addDebugLog(logMessage);
                        break;
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        initializeData();
        setupClickListeners();
        requestNotificationPermission();
        
        // Register broadcast receiver for service status updates
        IntentFilter filter = new IntentFilter();
        filter.addAction(NotificationService.ACTION_SERVICE_STATUS);
        filter.addAction(NotificationService.ACTION_WEBSOCKET_STATUS);
        filter.addAction(NotificationService.ACTION_DEBUG_LOG);
        registerReceiver(serviceStatusReceiver, filter);

        addDebugLog("MainActivity initialized");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(serviceStatusReceiver);
    }

    private void initializeViews() {
        etWebSocketUrl = findViewById(R.id.etWebSocketUrl);
        tvServiceStatus = findViewById(R.id.tvServiceStatus);
        tvWebSocketStatus = findViewById(R.id.tvWebSocketStatus);
        tvDebugLog = findViewById(R.id.tvDebugLog);
        btnSaveUrl = findViewById(R.id.btnSaveUrl);
        btnTestConnection = findViewById(R.id.btnTestConnection);
        btnStartService = findViewById(R.id.btnStartService);
        btnStopService = findViewById(R.id.btnStopService);
        btnClearLog = findViewById(R.id.btnClearLog);
        btnSendTestNotification = findViewById(R.id.btnSendTestNotification);

        tvDebugLog.setMovementMethod(new ScrollingMovementMethod());
    }

    private void initializeData() {
        sharedPreferences = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        mainHandler = new Handler(Looper.getMainLooper());
        debugLogBuilder = new StringBuilder();

        // Load saved WebSocket URL
        String savedUrl = sharedPreferences.getString(KEY_WEBSOCKET_URL, DEFAULT_WEBSOCKET_URL);
        etWebSocketUrl.setText(savedUrl);
    }

    private void setupClickListeners() {
        btnSaveUrl.setOnClickListener(v -> saveWebSocketUrl());
        btnTestConnection.setOnClickListener(v -> testConnection());
        btnStartService.setOnClickListener(v -> startNotificationService());
        btnStopService.setOnClickListener(v -> stopNotificationService());
        btnClearLog.setOnClickListener(v -> clearDebugLog());
        btnSendTestNotification.setOnClickListener(v -> sendTestNotification());
    }

    private void saveWebSocketUrl() {
        String url = etWebSocketUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a valid WebSocket URL", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!url.startsWith("ws://") && !url.startsWith("wss://")) {
            Toast.makeText(this, "URL must start with ws:// or wss://", Toast.LENGTH_SHORT).show();
            return;
        }

        sharedPreferences.edit().putString(KEY_WEBSOCKET_URL, url).apply();
        addDebugLog("WebSocket URL saved: " + url);
        Toast.makeText(this, "WebSocket URL saved successfully", Toast.LENGTH_SHORT).show();

        // Notify service about URL change
        Intent intent = new Intent(this, NotificationService.class);
        intent.setAction(NotificationService.ACTION_UPDATE_URL);
        intent.putExtra("websocket_url", url);
        startForegroundService(intent);
    }

    private void testConnection() {
        String url = etWebSocketUrl.getText().toString().trim();
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a WebSocket URL first", Toast.LENGTH_SHORT).show();
            return;
        }

        addDebugLog("Testing connection to: " + url);
        Intent intent = new Intent(this, NotificationService.class);
        intent.setAction(NotificationService.ACTION_TEST_CONNECTION);
        intent.putExtra("websocket_url", url);
        startForegroundService(intent);
    }

    private void startNotificationService() {
        addDebugLog("Starting notification service...");
        Intent serviceIntent = new Intent(this, NotificationService.class);
        startForegroundService(serviceIntent);
    }

    private void stopNotificationService() {
        addDebugLog("Stopping notification service...");
        Intent serviceIntent = new Intent(this, NotificationService.class);
        stopService(serviceIntent);
    }

    private void sendTestNotification() {
        addDebugLog("Sending test notification...");
        Intent intent = new Intent(this, NotificationService.class);
        intent.setAction(NotificationService.ACTION_SEND_TEST_NOTIFICATION);
        startForegroundService(intent);
    }

    private void clearDebugLog() {
        debugLogBuilder.setLength(0);
        tvDebugLog.setText("Debug log cleared...");
    }

    private void addDebugLog(String message) {
        String timestamp = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String logEntry = "[" + timestamp + "] " + message + "\n";
        
        debugLogBuilder.append(logEntry);
        
        // Keep only last 50 lines to prevent memory issues
        String[] lines = debugLogBuilder.toString().split("\n");
        if (lines.length > 50) {
            debugLogBuilder.setLength(0);
            for (int i = lines.length - 50; i < lines.length; i++) {
                debugLogBuilder.append(lines[i]).append("\n");
            }
        }

        mainHandler.post(() -> {
            tvDebugLog.setText(debugLogBuilder.toString());
            // Auto-scroll to bottom
            int scrollAmount = tvDebugLog.getLayout() != null ? 
                tvDebugLog.getLayout().getLineTop(tvDebugLog.getLineCount()) - tvDebugLog.getHeight() : 0;
            if (scrollAmount > 0) {
                tvDebugLog.scrollTo(0, scrollAmount);
            }
        });
    }

    private void updateServiceStatus(String status) {
        mainHandler.post(() -> {
            tvServiceStatus.setText(status);
            int color = status.equals("Running") ? 
                getResources().getColor(android.R.color.holo_green_dark) :
                getResources().getColor(android.R.color.holo_red_dark);
            tvServiceStatus.setTextColor(color);
        });
    }

    private void updateWebSocketStatus(String status) {
        mainHandler.post(() -> {
            tvWebSocketStatus.setText(status);
            int color;
            switch (status) {
                case "Connected":
                    color = getResources().getColor(android.R.color.holo_green_dark);
                    break;
                case "Connecting":
                    color = getResources().getColor(android.R.color.holo_orange_dark);
                    break;
                default:
                    color = getResources().getColor(android.R.color.holo_red_dark);
                    break;
            }
            tvWebSocketStatus.setTextColor(color);
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) 
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, 
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 
                    NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                addDebugLog("Notification permission granted");
                startNotificationService();
            } else {
                addDebugLog("Notification permission denied");
                Toast.makeText(this, "Notification permission is required for the app to function properly", 
                    Toast.LENGTH_LONG).show();
            }
        }
    }
}