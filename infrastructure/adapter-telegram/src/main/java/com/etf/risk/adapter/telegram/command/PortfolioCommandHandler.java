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

import java.util.List;

@Component
@RequiredArgsConstructor
public class PortfolioCommandHandler implements CommandHandler {

    private final RegisterUserUseCase registerUserUseCase;
    private final ManagePortfolioUseCase managePortfolioUseCase;

    @Override
    public String getCommand() {
        return "/portfolio";
    }

    @Override
    public String getDescription() {
        return "포트폴리오 조회";
    }

    @Override
    public String handle(Update update) {
        Long chatId = update.getMessage().getChatId();
        TelegramChatId telegramChatId = new TelegramChatId(chatId);

        User user = registerUserUseCase.findUserByChatId(telegramChatId);
        if (user == null) {
            return "등록되지 않은 사용자입니다. /register 명령어로 먼저 등록해주세요.";
        }

        try {
            List<Position> positions = managePortfolioUseCase.getUserPositions(user.getId());

            if (positions.isEmpty()) {
                return """
                    포트폴리오가 비어있습니다.

                    /add 명령어로 포지션을 추가하세요.
                    예: /add GOF 100 20.5""";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("내 포트폴리오\n");
            sb.append("═══════════════════\n\n");

            for (Position position : positions) {
                sb.append(String.format("[%s]\n", position.getSymbol()));
                sb.append(String.format("  수량: %d주\n", position.getQuantity()));
                sb.append(String.format("  평단가: $%s\n", formatMoney(position.getAveragePrice())));
                sb.append(String.format("  투자금: $%s\n", formatMoney(position.getAveragePrice().multiply(position.getQuantity()))));
                sb.append("\n");
            }

            sb.append("═══════════════════\n");
            sb.append("/risk 명령어로 리스크 분석을 확인하세요.");

            return sb.toString();
        } catch (Exception e) {
            return "포트폴리오 조회 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String formatMoney(Money money) {
        return String.format("%.2f", money.getAmount().doubleValue());
    }
}
