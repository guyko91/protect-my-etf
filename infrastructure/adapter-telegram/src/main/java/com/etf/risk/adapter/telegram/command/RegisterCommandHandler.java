package com.etf.risk.adapter.telegram.command;

import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class RegisterCommandHandler implements CommandHandler {

    private final RegisterUserUseCase registerUserUseCase;

    @Override
    public String getCommand() {
        return "/register";
    }

    @Override
    public String getDescription() {
        return "사용자 등록";
    }

    @Override
    public String handle(Update update) {
        Long chatId = update.getMessage().getChatId();
        String username = update.getMessage().getFrom().getUserName();
        TelegramChatId telegramChatId = new TelegramChatId(chatId);

        if (registerUserUseCase.isUserRegistered(telegramChatId)) {
            return "이미 등록된 사용자입니다. /portfolio 명령어로 포트폴리오를 확인하세요.";
        }

        try {
            User user = registerUserUseCase.registerUser(telegramChatId, username);
            return String.format("""
                등록이 완료되었습니다!

                사용자 ID: %d
                텔레그램 ID: @%s

                이제 /add 명령어로 포지션을 추가하세요.
                예: /add GOF 100 20.5""", user.getId(), username != null ? username : "없음");
        } catch (Exception e) {
            return "등록 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}
