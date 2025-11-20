package com.etf.risk.domain.port.in;

import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;

public interface RegisterUserUseCase {
    User registerUser(TelegramChatId chatId, String username);
    User findUserByChatId(TelegramChatId chatId);
    boolean isUserRegistered(TelegramChatId chatId);
}
