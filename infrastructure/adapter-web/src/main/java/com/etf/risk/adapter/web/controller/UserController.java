package com.etf.risk.adapter.web.controller;

import com.etf.risk.adapter.web.common.ApiResponse;
import com.etf.risk.adapter.web.dto.user.RegisterUserRequest;
import com.etf.risk.adapter.web.dto.user.UserResponse;
import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.RegisterUserUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final RegisterUserUseCase registerUserUseCase;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserResponse> registerUser(@Valid @RequestBody RegisterUserRequest request) {
        TelegramChatId chatId = new TelegramChatId(request.getTelegramChatId());
        User user = registerUserUseCase.registerUser(chatId, request.getUsername());
        return ApiResponse.success(UserResponse.from(user), "사용자 등록이 완료되었습니다");
    }

    @GetMapping("/chat/{chatId}")
    public ApiResponse<UserResponse> getUserByChatId(@PathVariable Long chatId) {
        TelegramChatId telegramChatId = new TelegramChatId(chatId);
        User user = registerUserUseCase.findUserByChatId(telegramChatId);
        return ApiResponse.success(UserResponse.from(user));
    }

    @GetMapping("/chat/{chatId}/exists")
    public ApiResponse<Boolean> isUserRegistered(@PathVariable Long chatId) {
        TelegramChatId telegramChatId = new TelegramChatId(chatId);
        boolean exists = registerUserUseCase.isUserRegistered(telegramChatId);
        return ApiResponse.success(exists);
    }
}
