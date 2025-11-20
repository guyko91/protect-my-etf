package com.etf.risk.adapter.web.dto.user;

import com.etf.risk.domain.model.user.User;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class UserResponse {

    private final Long id;
    private final Long telegramChatId;
    private final String telegramUsername;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private UserResponse(Long id, Long telegramChatId, String telegramUsername,
                        LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.telegramChatId = telegramChatId;
        this.telegramUsername = telegramUsername;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static UserResponse from(User user) {
        return new UserResponse(
            user.getId(),
            user.getTelegramChatId().value(),
            user.getTelegramUsername(),
            user.getCreatedAt(),
            user.getUpdatedAt()
        );
    }
}
