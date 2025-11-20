package com.etf.risk.adapter.web.dto.user;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class RegisterUserRequest {

    @NotNull(message = "텔레그램 Chat ID는 필수입니다")
    @Positive(message = "텔레그램 Chat ID는 양수여야 합니다")
    private Long telegramChatId;

    private String username;

    public RegisterUserRequest(Long telegramChatId, String username) {
        this.telegramChatId = telegramChatId;
        this.username = username;
    }
}
