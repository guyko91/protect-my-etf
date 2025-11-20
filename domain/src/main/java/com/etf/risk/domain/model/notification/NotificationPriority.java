package com.etf.risk.domain.model.notification;

public enum NotificationPriority {
    LOW("낮음"),
    NORMAL("보통"),
    HIGH("높음"),
    URGENT("긴급");

    private final String displayName;

    NotificationPriority(String displayName) {
        this.displayName = displayName;
    }

    public String displayName() {
        return displayName;
    }
}
