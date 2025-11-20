package com.etf.risk.domain.model.notification;

import com.etf.risk.domain.model.user.TelegramChatId;

import java.time.LocalDateTime;

public class NotificationMessage {
    private final TelegramChatId chatId;
    private final String title;
    private final String content;
    private final NotificationPriority priority;
    private final LocalDateTime createdAt;

    private NotificationMessage(TelegramChatId chatId, String title, String content,
                                NotificationPriority priority, LocalDateTime createdAt) {
        this.chatId = chatId;
        this.title = title;
        this.content = content;
        this.priority = priority;
        this.createdAt = createdAt;
    }

    public static NotificationMessage create(TelegramChatId chatId, String title,
                                             String content, NotificationPriority priority) {
        validateInputs(chatId, title, content, priority);
        return new NotificationMessage(chatId, title, content, priority, LocalDateTime.now());
    }

    private static void validateInputs(TelegramChatId chatId, String title,
                                       String content, NotificationPriority priority) {
        if (chatId == null) {
            throw new IllegalArgumentException("Chat ID는 필수입니다");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("제목은 필수입니다");
        }
        if (content == null || content.isBlank()) {
            throw new IllegalArgumentException("내용은 필수입니다");
        }
        if (priority == null) {
            throw new IllegalArgumentException("우선순위는 필수입니다");
        }
    }

    public String formatForTelegram() {
        StringBuilder sb = new StringBuilder();
        sb.append("*").append(title).append("*\n\n");
        sb.append(content);
        return sb.toString();
    }

    public boolean isHighPriority() {
        return priority == NotificationPriority.HIGH || priority == NotificationPriority.URGENT;
    }

    public TelegramChatId chatId() {
        return chatId;
    }

    public String title() {
        return title;
    }

    public String content() {
        return content;
    }

    public NotificationPriority priority() {
        return priority;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }
}
