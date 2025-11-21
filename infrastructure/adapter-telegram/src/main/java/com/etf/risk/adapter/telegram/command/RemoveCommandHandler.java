package com.etf.risk.adapter.telegram.command;

import com.etf.risk.domain.model.portfolio.Position;
import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.ManagePortfolioUseCase;
import com.etf.risk.domain.port.in.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
@RequiredArgsConstructor
public class RemoveCommandHandler implements CommandHandler {

    private final RegisterUserUseCase registerUserUseCase;
    private final ManagePortfolioUseCase managePortfolioUseCase;

    @Override
    public String getCommand() {
        return "/remove";
    }

    @Override
    public String getDescription() {
        return "포지션 제거";
    }

    @Override
    public String handle(Update update) {
        Long chatId = update.getMessage().getChatId();
        String text = update.getMessage().getText();
        TelegramChatId telegramChatId = new TelegramChatId(chatId);

        User user = registerUserUseCase.findUserByChatId(telegramChatId);
        if (user == null) {
            return "등록되지 않은 사용자입니다. /register 명령어로 먼저 등록해주세요.";
        }

        String[] parts = text.split("\\s+");
        if (parts.length != 2) {
            return """
                사용법: /remove [심볼]
                예: /remove GOF

                현재 보유 포지션을 전량 매도합니다.
                /portfolio 명령어로 보유 현황을 먼저 확인하세요.""";
        }

        String symbol = parts[1].toUpperCase();

        try {
            Position position = managePortfolioUseCase.getUserPosition(user.getId(), symbol);

            if (position == null) {
                return String.format("[%s] 포지션을 보유하고 있지 않습니다.", symbol);
            }

            int quantity = position.getQuantity();

            managePortfolioUseCase.removePosition(user.getId(), symbol);

            return String.format("""
                [%s] 포지션 제거 완료!

                매도 수량: %d주

                /portfolio 명령어로 업데이트된 포트폴리오를 확인하세요.""", symbol, quantity);
        } catch (Exception e) {
            return "포지션 제거 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}
