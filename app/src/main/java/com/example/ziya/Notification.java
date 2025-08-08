package com.example.ziya;

public class Notification {
    private final String type;
    private final String title;
    private final String message;
    private final String time;
    private boolean isRead;

    public Notification(String type, String title, String message, String time, boolean isRead) {
        this.type = type;
        this.title = title;
        this.message = message;
        this.time = time;
        this.isRead = isRead;
    }

    public String getType() {
        return type;
    }

    public String getTitle() {
        return title;
    }

    public String getMessage() {
        return message;
    }

    public String getTime() {
        return time;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }
}
