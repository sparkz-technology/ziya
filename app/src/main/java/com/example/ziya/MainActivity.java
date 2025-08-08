// package com.example.ziya;

// import android.Manifest;
// import android.content.Intent;
// import android.content.pm.PackageManager;
// import android.os.Build;
// import android.os.Bundle;
// import androidx.activity.result.ActivityResultLauncher;
// import androidx.activity.result.contract.ActivityResultContracts;
// import androidx.appcompat.app.AppCompatActivity;
// import androidx.core.content.ContextCompat;

// public class MainActivity extends AppCompatActivity {

//     // Declare the launcher at the top of your Activity/Fragment:
//     private final ActivityResultLauncher<String> requestPermissionLauncher =
//             registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
//                 if (isGranted) {
//                     // Permission is granted. You can now start the service.
//                     startNotificationService();
//                 } else {
//                     // Explain to the user that the feature is unavailable because the
//                     // feature requires a permission that the user has denied.
//                     // You can show a dialog or a snackbar here.
//                 }
//             });

//     @Override
//     protected void onCreate(Bundle savedInstanceState) {
//         super.onCreate(savedInstanceState);
//         setContentView(R.layout.activity_main);
//         askNotificationPermission();
//     }

//     private void askNotificationPermission() {
//         // This is only necessary for API level 33 and higher.
//         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//             if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
//                     PackageManager.PERMISSION_GRANTED) {
//                 // Permission is already granted, start the service.
//                 startNotificationService();
//             } else {
//                 // Directly ask for the permission.
//                 // The registered ActivityResultCallback gets the result of this request.
//                 requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
//             }
//         } else {
//             // For older versions, permission is not required, so just start the service.
//             startNotificationService();
//         }
//     }

//     private void startNotificationService() {
//         // Start the foreground service
//         Intent serviceIntent = new Intent(this, NotificationService.class);
//         startForegroundService(serviceIntent);
//     }
// }
package com.example.ziya;

import android.content.res.Configuration;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView notificationRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> allNotifications;
    private String currentFilter = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        loadNotifications();
        setupRecyclerView();
        setupFilterButtons();
        filterAndDisplayNotifications(); // Initial filter
    }

    private void setupToolbar() {
        ImageButton themeButton = findViewById(R.id.theme_button);
        updateThemeIcon(themeButton);
        themeButton.setOnClickListener(v -> {
            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });

        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> showSettingsBottomSheet());
    }

    private void updateThemeIcon(ImageButton themeButton) {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
            themeButton.setImageResource(R.drawable.ic_sun);
        } else {
            themeButton.setImageResource(R.drawable.ic_moon);
        }
    }

    private void loadNotifications() {
        allNotifications = new ArrayList<>();
        allNotifications.add(new Notification("success", "Connection successful", "Your profile has been updated successfully.", "2m ago", false));
        allNotifications.add(new Notification("error", "Payment Failed", "Could not process payment.", "15m ago", false));
        allNotifications.add(new Notification("warning", "API Key Deprecated", "Your API key will expire in 3 days.", "1d ago", true));
        allNotifications.add(new Notification("info", "System Maintenance", "Scheduled maintenance upcoming.", "3d ago", true));
        allNotifications.add(new Notification("warning", "Low Disk Space", "Your storage is almost full.", "4d ago", false));
        allNotifications.add(new Notification("success", "New Login", "A new device has logged into your account.", "5d ago", true));
    }

    private void setupRecyclerView() {
        notificationRecyclerView = findViewById(R.id.notification_recycler_view);
        notificationRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        notificationAdapter = new NotificationAdapter(new ArrayList<>(), this::showNotificationDetail);
        notificationRecyclerView.setAdapter(notificationAdapter);
    }

    private void setupFilterButtons() {
        LinearLayout filterContainer = findViewById(R.id.filter_container);
        String[] filters = {"all", "success", "error", "warning", "info"};
        int[] filterIcons = {
                R.drawable.ic_filter_all, R.drawable.ic_notification_success, R.drawable.ic_notification_error,
                R.drawable.ic_notification_warning, R.drawable.ic_notification_info
        };

        filterContainer.removeAllViews();

        for (int i = 0; i < filters.length; i++) {
            ImageButton button = new ImageButton(this);
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f);
            button.setLayoutParams(params);
            button.setImageResource(filterIcons[i]);
            button.setBackgroundResource(R.drawable.filter_button_background);
            button.setPadding(24, 24, 24, 24);

            final String filter = filters[i];
            button.setOnClickListener(v -> {
                currentFilter = filter;
                updateFilterButtons();
                filterAndDisplayNotifications();
            });
            filterContainer.addView(button);
        }
        updateFilterButtons();
    }

    private void updateFilterButtons() {
        LinearLayout filterContainer = findViewById(R.id.filter_container);
        for (int i = 0; i < filterContainer.getChildCount(); i++) {
            View child = filterContainer.getChildAt(i);
            String[] filters = {"all", "success", "error", "warning", "info"};
            child.setSelected(filters[i].equals(currentFilter));
        }
    }

    private void filterAndDisplayNotifications() {
        if (currentFilter.equals("all")) {
            notificationAdapter.updateNotifications(allNotifications);
        } else {
            List<Notification> filteredList = allNotifications.stream()
                    .filter(n -> n.getType().equals(currentFilter))
                    .collect(Collectors.toList());
            notificationAdapter.updateNotifications(filteredList);
        }
    }

    private void showSettingsBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        bottomSheetDialog.setContentView(R.layout.bottom_sheet_settings);
        bottomSheetDialog.findViewById(R.id.save_button).setOnClickListener(v -> {
            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });
        bottomSheetDialog.show();
    }

    private void showNotificationDetail(Notification notification) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_notification_detail, null);
        bottomSheetDialog.setContentView(sheetView);

        TextView title = sheetView.findViewById(R.id.detail_title);
        TextView message = sheetView.findViewById(R.id.detail_message);
        TextView time = sheetView.findViewById(R.id.detail_timestamp);
        ImageView icon = sheetView.findViewById(R.id.detail_icon);

        title.setText(notification.getTitle());
        message.setText(notification.getMessage());
        time.setText("Received " + notification.getTime());

        int iconRes = getIconResource(notification.getType());
        int colorRes = getIconColor(notification.getType());
        icon.setImageResource(iconRes);
        icon.setColorFilter(ContextCompat.getColor(this, colorRes));

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationAdapter.notifyDataSetChanged();
        }

        bottomSheetDialog.show();
    }

    public static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private List<Notification> notifications;
        private final OnItemClickListener listener;

        public interface OnItemClickListener {
            void onItemClick(Notification notification);
        }

        public NotificationAdapter(List<Notification> notifications, OnItemClickListener listener) {
            this.notifications = notifications;
            this.listener = listener;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.bind(notifications.get(position), listener);
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        public void updateNotifications(List<Notification> newNotifications) {
            this.notifications = new ArrayList<>(newNotifications);
            notifyDataSetChanged();
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView title, message, time;
            private final ImageView icon;
            private final View unreadDot;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.notification_title);
                message = itemView.findViewById(R.id.notification_message);
                time = itemView.findViewById(R.id.notification_time);
                icon = itemView.findViewById(R.id.notification_icon);
                unreadDot = itemView.findViewById(R.id.unread_dot);
            }

            public void bind(final Notification notification, final OnItemClickListener listener) {
                title.setText(notification.getTitle());
                message.setText(notification.getMessage());
                time.setText(notification.getTime());
                unreadDot.setVisibility(notification.isRead() ? View.INVISIBLE : View.VISIBLE);
                int iconRes = getIconResource(notification.getType());
                int colorRes = getIconColor(notification.getType());
                icon.setImageResource(iconRes);
                icon.setColorFilter(ContextCompat.getColor(itemView.getContext(), colorRes));
                itemView.setOnClickListener(v -> listener.onItemClick(notification));
            }
        }
    }

    private static int getIconResource(String type) {
        switch (type) {
            case "success": return R.drawable.ic_notification_success;
            case "error": return R.drawable.ic_notification_error;
            case "warning": return R.drawable.ic_notification_warning;
            default: return R.drawable.ic_notification_info;
        }
    }

    private static int getIconColor(String type) {
        switch (type) {
            case "success": return R.color.notif_success;
            case "error": return R.color.notif_error;
            case "warning": return R.color.notif_warning;
            default: return R.color.notif_info;
        }
    }
}
