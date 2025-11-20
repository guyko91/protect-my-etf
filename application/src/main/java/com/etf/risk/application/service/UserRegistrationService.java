package com.etf.risk.application.service;

import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.RegisterUserUseCase;
import com.etf.risk.domain.port.out.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserRegistrationService implements RegisterUserUseCase {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public User registerUser(TelegramChatId chatId, String username) {
        if (isUserRegistered(chatId)) {
            throw new IllegalStateException("이미 등록된 사용자입니다: " + chatId.value());
        }

        User newUser = User.register(chatId, username);
        return userRepository.save(newUser);
    }

    @Override
    public User findUserByChatId(TelegramChatId chatId) {
        return userRepository.findByTelegramChatId(chatId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + chatId.value()));
    }

    @Override
    public boolean isUserRegistered(TelegramChatId chatId) {
        return userRepository.existsByTelegramChatId(chatId);
    }
}
