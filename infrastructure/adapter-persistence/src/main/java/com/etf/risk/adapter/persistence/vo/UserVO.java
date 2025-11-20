package com.etf.risk.adapter.persistence.vo;

import java.time.LocalDateTime;

public record UserVO(
    Long id,
    Long telegramChatId,
    String telegramUsername,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
