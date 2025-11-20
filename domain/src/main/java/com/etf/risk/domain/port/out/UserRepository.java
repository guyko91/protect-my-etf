package com.etf.risk.domain.port.out;

import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;

import java.util.List;
import java.util.Optional;

public interface UserRepository {
    User save(User user);
    Optional<User> findById(Long id);
    Optional<User> findByTelegramChatId(TelegramChatId chatId);
    List<User> findUsersWithETF(String etfSymbol);
    boolean existsByTelegramChatId(TelegramChatId chatId);
}
