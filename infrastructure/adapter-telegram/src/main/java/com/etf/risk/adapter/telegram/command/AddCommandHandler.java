package com.etf.risk.adapter.telegram.command;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.portfolio.Position;
import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.ManagePortfolioUseCase;
import com.etf.risk.domain.port.in.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.math.BigDecimal;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class AddCommandHandler implements CommandHandler {

    private static final Set<String> SUPPORTED_SYMBOLS = Set.of("GOF", "QQQI");

    private final RegisterUserUseCase registerUserUseCase;
    private final ManagePortfolioUseCase managePortfolioUseCase;

    @Override
    public String getCommand() {
        return "/add";
    }

    @Override
    public String getDescription() {
        return "포지션 추가";
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
        if (parts.length != 4) {
            return """
                사용법: /add [심볼] [수량] [평단가]
                예: /add GOF 100 20.5

                지원 ETF: GOF, QQQI""";
        }

        String symbol = parts[1].toUpperCase();
        if (!SUPPORTED_SYMBOLS.contains(symbol)) {
            return "지원하지 않는 ETF입니다. 지원 ETF: GOF, QQQI";
        }

        int quantity;
        BigDecimal price;
        try {
            quantity = Integer.parseInt(parts[2]);
            if (quantity <= 0) {
                return "수량은 양수여야 합니다.";
            }
        } catch (NumberFormatException e) {
            return "수량이 올바르지 않습니다. 정수를 입력하세요.";
        }

        try {
            price = new BigDecimal(parts[3]);
            if (price.compareTo(BigDecimal.ZERO) <= 0) {
                return "가격은 양수여야 합니다.";
            }
        } catch (NumberFormatException e) {
            return "가격이 올바르지 않습니다. 숫자를 입력하세요.";
        }

        try {
            Position existingPosition = managePortfolioUseCase.getUserPosition(user.getId(), symbol);

            if (existingPosition != null) {
                managePortfolioUseCase.addToPosition(user.getId(), symbol, quantity, Money.of(price));
                return String.format("""
                    [%s] 추가 매수 완료!

                    추가 수량: %d주
                    매수가: $%.2f

                    /portfolio 명령어로 업데이트된 포트폴리오를 확인하세요.""", symbol, quantity, price);
            } else {
                managePortfolioUseCase.addPosition(user.getId(), symbol, quantity, Money.of(price));
                return String.format("""
                    [%s] 포지션 추가 완료!

                    수량: %d주
                    평단가: $%.2f
                    투자금: $%.2f

                    /portfolio 명령어로 포트폴리오를 확인하세요.""", symbol, quantity, price, price.multiply(BigDecimal.valueOf(quantity)));
            }
        } catch (Exception e) {
            return "포지션 추가 중 오류가 발생했습니다: " + e.getMessage();
        }
    }
}
