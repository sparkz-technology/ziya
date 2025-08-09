package com.example.ziya;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.res.Configuration;
import android.content.res.ColorStateList;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.OvershootInterpolator;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.view.ActionMode;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class MainActivity extends AppCompatActivity {

    private RecyclerView notificationRecyclerView;
    private NotificationAdapter notificationAdapter;
    private List<Notification> allNotifications;
    private String currentFilter = "all";
    private boolean isSelectionMode = false;
    private Set<Notification> selectedNotifications = new HashSet<>();
    private ActionMode actionMode;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private View emptyStateLayout;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupToolbar();
        loadNotifications();
        setupRecyclerView();
        setupSwipeRefresh();
        setupEmptyState();
        setupFilterButtons();
        filterAndDisplayNotifications(); // Initial filter
    }

    private void setupToolbar() {
        ImageButton themeButton = findViewById(R.id.theme_button);
        updateThemeIcon(themeButton);
        themeButton.setOnClickListener(v -> {
            animateButtonPress(themeButton);

            int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            }
        });

        ImageButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(v -> {
            animateButtonPress(settingsButton);
            showSettingsBottomSheet();
        });
    }

    private void updateThemeIcon(ImageButton themeButton) {
        int currentNightMode = getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK;

        ObjectAnimator scaleDown = ObjectAnimator.ofFloat(themeButton, "scaleX", 1f, 0.8f);
        scaleDown.setDuration(100);
        ObjectAnimator scaleUp = ObjectAnimator.ofFloat(themeButton, "scaleX", 0.8f, 1f);
        scaleUp.setDuration(100);

        scaleDown.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (currentNightMode == Configuration.UI_MODE_NIGHT_YES) {
                    themeButton.setImageResource(R.drawable.ic_sun);
                } else {
                    themeButton.setImageResource(R.drawable.ic_moon);
                }
                scaleUp.start();
            }
        });

        scaleDown.start();
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

        DefaultItemAnimator animator = new DefaultItemAnimator();
        animator.setAddDuration(300);
        animator.setRemoveDuration(300);
        animator.setMoveDuration(300);
        animator.setChangeDuration(300);
        notificationRecyclerView.setItemAnimator(animator);

        notificationAdapter = new NotificationAdapter(new ArrayList<>(), this::onItemClick, this::onItemLongClick, selectedNotifications);
        notificationRecyclerView.setAdapter(notificationAdapter);

        setupSwipeToDelete();
    }

    // Corrected swipe-to-delete implementation
    private void setupSwipeToDelete() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    Notification notification = notificationAdapter.getNotifications().get(position);

                    // Remove from the main list first
                    allNotifications.remove(notification);
                    // Then remove from the adapter's current list
                    notificationAdapter.getNotifications().remove(position);
                    // Finally, notify the adapter about the item removal at that specific position
                    notificationAdapter.notifyItemRemoved(position);

                    // Show a snackbar with an undo option
                    com.google.android.material.snackbar.Snackbar.make(findViewById(android.R.id.content), "Notification deleted", com.google.android.material.snackbar.Snackbar.LENGTH_LONG)
                        .setAction("UNDO", v -> {
                            // Add the notification back to the main list at its original position
                            allNotifications.add(position, notification);
                            // Re-filter and display to ensure consistency
                            filterAndDisplayNotifications();
                        })
                        .show();
                }
            }

            @Override
            public void onChildDraw(@NonNull android.graphics.Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    View itemView = viewHolder.itemView;
                    final float alpha = 1.0f - Math.abs(dX) / (float) itemView.getWidth();
                    itemView.setAlpha(alpha);
                    itemView.setTranslationX(dX);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                viewHolder.itemView.setAlpha(1.0f);
                viewHolder.itemView.setTranslationX(0f);
            }
        }).attachToRecyclerView(notificationRecyclerView);
    }

    private void setupSwipeRefresh() {
        swipeRefreshLayout = findViewById(R.id.swipe_refresh_layout);
        swipeRefreshLayout.setColorSchemeResources(
            R.color.notif_success,
            R.color.notif_info,
            R.color.notif_warning,
            R.color.notif_error
        );

        swipeRefreshLayout.setOnRefreshListener(this::refreshNotifications);
    }

    private void refreshNotifications() {
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            List<String[]> newNotifications = java.util.Arrays.asList(
                new String[]{"success", "Sync Complete", "Your data has been synchronized.", "Just now"},
                new String[]{"info", "New Update Available", "Version 2.0 is now available for download.", "1m ago"}
            );

            for (String[] notif : newNotifications) {
                allNotifications.add(0, new Notification(notif[0], notif[1], notif[2], notif[3], false));
            }

            filterAndDisplayNotifications();
            swipeRefreshLayout.setRefreshing(false);

            Toast.makeText(this, "Notifications updated", Toast.LENGTH_SHORT).show();
        }, 1500);
    }

    private void setupEmptyState() {
        emptyStateLayout = findViewById(R.id.empty_state_layout);
        findViewById(R.id.refresh_button).setOnClickListener(v -> {
            animateButtonPress(v);
            swipeRefreshLayout.setRefreshing(true);
            refreshNotifications();
        });
    }

    private void onItemClick(Notification notification) {
        if (isSelectionMode) {
            toggleSelection(notification);
        } else {
            showNotificationDetail(notification);
        }
    }

    private void onItemLongClick(Notification notification) {
        if (!isSelectionMode) {
            isSelectionMode = true;
            actionMode = startSupportActionMode(actionModeCallback);
        }
        toggleSelection(notification);
    }

    private void toggleSelection(Notification notification) {
        if (selectedNotifications.contains(notification)) {
            selectedNotifications.remove(notification);
        } else {
            selectedNotifications.add(notification);
        }
        if (selectedNotifications.isEmpty()) {
            if (actionMode != null) {
                actionMode.finish();
            }
        } else {
            if (actionMode != null) {
                actionMode.invalidate();
            }
        }
        notificationAdapter.notifyDataSetChanged();
    }

    private final ActionMode.Callback actionModeCallback = new ActionMode.Callback() {
        @Override
        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.contextual_action_menu, menu);
            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            int selectionCount = selectedNotifications.size();
            mode.setTitle(selectionCount + " selected");

            MenuItem selectAllItem = menu.findItem(R.id.action_select_all);
            if (selectionCount == notificationAdapter.getItemCount()) {
                selectAllItem.setTitle("Deselect All");
            } else {
                selectAllItem.setTitle("Select All");
            }
            return true;
        }

        @Override
        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            int itemId = item.getItemId();
            if (itemId == R.id.action_delete) {
                allNotifications.removeAll(selectedNotifications);
                filterAndDisplayNotifications();
                mode.finish();
                return true;
            } else if (itemId == R.id.action_mark_read) {
                for (Notification notification : selectedNotifications) {
                    notification.setRead(true);
                }
                filterAndDisplayNotifications();
                mode.finish();
                return true;
            } else if (itemId == R.id.action_select_all) {
                if (selectedNotifications.size() == notificationAdapter.getItemCount()) {
                    selectedNotifications.clear();
                } else {
                    selectedNotifications.clear();
                    selectedNotifications.addAll(notificationAdapter.getNotifications());
                }
                notificationAdapter.notifyDataSetChanged();
                mode.invalidate();
                return true;
            }
            return false;
        }

        @Override
        public void onDestroyActionMode(ActionMode mode) {
            isSelectionMode = false;
            selectedNotifications.clear();
            notificationAdapter.notifyDataSetChanged();
            actionMode = null;
        }
    };

    private void setupFilterButtons() {
        ChipGroup filterContainer = findViewById(R.id.filter_container);
        String[] filters = {"all", "success", "error", "warning", "info"};
        String[] filterNames = {"All", "Success", "Error", "Warning", "Info"};

        filterContainer.removeAllViews();

        for (int i = 0; i < filters.length; i++) {
            final String filter = filters[i];
            final String filterName = filterNames[i];

            Chip chip = new Chip(this);
            chip.setText(filterName);
            chip.setCheckable(true);
            chip.setClickable(true);
            chip.setChecked(filter.equals(currentFilter));
            chip.setOnClickListener(v -> {
                currentFilter = filter;
                filterAndDisplayNotifications();
                updateFilterButtons();
            });
            filterContainer.addView(chip);
        }
        updateFilterButtons();
    }

    private void updateFilterButtons() {
        ChipGroup filterContainer = findViewById(R.id.filter_container);

        for (int i = 0; i < filterContainer.getChildCount(); i++) {
            Chip chip = (Chip) filterContainer.getChildAt(i);
            String filterType = getFilterTypeForChip(chip);
            boolean isActive = filterType.equals(currentFilter);
            chip.setChecked(isActive);

            int activeColor;
            switch (filterType) {
                case "success":
                    activeColor = ContextCompat.getColor(this, R.color.notif_success);
                    break;
                case "error":
                    activeColor = ContextCompat.getColor(this, R.color.notif_error);
                    break;
                case "warning":
                    activeColor = ContextCompat.getColor(this, R.color.notif_warning);
                    break;
                case "info":
                    activeColor = ContextCompat.getColor(this, R.color.notif_info);
                    break;
                default:
                    TypedValue typedValue = new TypedValue();
                    getTheme().resolveAttribute(com.google.android.material.R.attr.colorPrimary, typedValue, true);
                    activeColor = typedValue.data;
                    break;
            }

            if (isActive) {
                chip.setChipBackgroundColor(ColorStateList.valueOf(activeColor));
                chip.setTextColor(ContextCompat.getColor(this, R.color.white));
            } else {
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorSurface, typedValue, true);
                chip.setChipBackgroundColor(ColorStateList.valueOf(typedValue.data));
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
                chip.setTextColor(typedValue.data);
            }
        }
    }

    private String getFilterTypeForChip(Chip chip) {
        String chipText = chip.getText().toString().toLowerCase();
        switch (chipText) {
            case "success":
                return "success";
            case "error":
                return "error";
            case "warning":
                return "warning";
            case "info":
                return "info";
            default:
                return "all";
        }
    }

    private void filterAndDisplayNotifications() {
        List<Notification> filteredList;
        if (currentFilter.equals("all")) {
            filteredList = new ArrayList<>(allNotifications);
        } else {
            filteredList = allNotifications.stream()
                    .filter(n -> n.getType().equals(currentFilter))
                    .collect(Collectors.toList());
        }
        notificationAdapter.updateNotifications(filteredList);

        if (filteredList.isEmpty()) {
            notificationRecyclerView.setVisibility(View.GONE);
            emptyStateLayout.setVisibility(View.VISIBLE);
            animateViewIn(emptyStateLayout);
        } else {
            emptyStateLayout.setVisibility(View.GONE);
            notificationRecyclerView.setVisibility(View.VISIBLE);
        }
    }

    private void showSettingsBottomSheet() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_settings, null);
        bottomSheetDialog.setContentView(sheetView);

        animateViewIn(sheetView);

        sheetView.findViewById(R.id.save_button).setOnClickListener(v -> {
            animateButtonPress(v);
            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show();
            bottomSheetDialog.dismiss();
        });

        sheetView.findViewById(R.id.cancel_button).setOnClickListener(v -> {
            animateButtonPress(v);
            bottomSheetDialog.dismiss();
        });

        bottomSheetDialog.show();
    }

    private void showNotificationDetail(Notification notification) {
        final BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_notification_detail, null);
        bottomSheetDialog.setContentView(sheetView);

        animateViewIn(sheetView);

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

        sheetView.findViewById(R.id.close_button).setOnClickListener(v -> {
            animateButtonPress(v);
            bottomSheetDialog.dismiss();
        });

        if (!notification.isRead()) {
            notification.setRead(true);
            notificationAdapter.notifyDataSetChanged();
        }

        bottomSheetDialog.show();
    }

    private void animateButtonPress(View button) {
        ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f);
        ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f);
        ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(button, "scaleX", 0.95f, 1f);
        ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(button, "scaleY", 0.95f, 1f);

        scaleDownX.setDuration(100);
        scaleDownY.setDuration(100);
        scaleUpX.setDuration(100);
        scaleUpY.setDuration(100);

        scaleUpX.setInterpolator(new OvershootInterpolator());
        scaleUpY.setInterpolator(new OvershootInterpolator());

        AnimatorSet scaleDown = new AnimatorSet();
        scaleDown.playTogether(scaleDownX, scaleDownY);

        AnimatorSet scaleUp = new AnimatorSet();
        scaleUp.playTogether(scaleUpX, scaleUpY);

        scaleDown.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                scaleUp.start();
            }
        });

        scaleDown.start();
    }

    private void animateViewIn(View view) {
        view.setAlpha(0f);
        view.setScaleX(0.8f);
        view.setScaleY(0.8f);

        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f);
        ObjectAnimator scaleInX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleInY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f);

        fadeIn.setDuration(300);
        scaleInX.setDuration(300);
        scaleInY.setDuration(300);

        scaleInX.setInterpolator(new OvershootInterpolator());
        scaleInY.setInterpolator(new OvershootInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeIn, scaleInX, scaleInY);
        animatorSet.start();
    }

    public static class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {
        private List<Notification> notifications;
        private final OnItemClickListener listener;
        private final OnItemLongClickListener longClickListener;
        private final Set<Notification> selectedItems;

        public interface OnItemClickListener {
            void onItemClick(Notification notification);
        }

        public interface OnItemLongClickListener {
            void onItemLongClick(Notification notification);
        }

        public NotificationAdapter(List<Notification> notifications, OnItemClickListener listener, OnItemLongClickListener longClickListener, Set<Notification> selectedItems) {
            this.notifications = notifications;
            this.listener = listener;
            this.longClickListener = longClickListener;
            this.selectedItems = selectedItems;
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item_notification, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Notification notification = notifications.get(position);
            boolean isSelected = selectedItems.contains(notification);
            holder.bind(notification, listener, longClickListener, isSelected);
        }

        @Override
        public int getItemCount() {
            return notifications.size();
        }

        public void updateNotifications(List<Notification> newNotifications) {
            this.notifications = new ArrayList<>(newNotifications);
            notifyDataSetChanged();
        }

        public List<Notification> getNotifications() {
            return notifications;
        }

        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView title, message, time;
            private final ImageView icon;
            private final View unreadIndicator;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                this.title = itemView.findViewById(R.id.notification_title);
                this.message = itemView.findViewById(R.id.notification_message);
                this.time = itemView.findViewById(R.id.notification_time);
                this.icon = itemView.findViewById(R.id.notification_icon);
                this.unreadIndicator = itemView.findViewById(R.id.unread_indicator);
            }

            public void bind(final Notification notification, final OnItemClickListener listener, final OnItemLongClickListener longClickListener, boolean isSelected) {
                View selectionOverlay = itemView.findViewById(R.id.selection_overlay);
                if (selectionOverlay != null) {
                    selectionOverlay.setVisibility(isSelected ? View.VISIBLE : View.GONE);
                }

                title.setText(notification.getTitle());
                message.setText(notification.getMessage());
                time.setText(notification.getTime());

                unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
                title.setTypeface(null, notification.isRead() ? Typeface.NORMAL : Typeface.BOLD);

                int iconRes = getIconResource(notification.getType());
                int colorRes = getIconColor(notification.getType());
                icon.setImageResource(iconRes);
                icon.setColorFilter(ContextCompat.getColor(itemView.getContext(), colorRes));

                itemView.setOnClickListener(v -> listener.onItemClick(notification));

                itemView.setOnLongClickListener(v -> {
                    longClickListener.onItemLongClick(notification);
                    return true;
                });
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