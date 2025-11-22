package com.etf.risk.adapter.telegram.command;

import com.etf.risk.domain.model.portfolio.Position;
import com.etf.risk.domain.model.risk.RiskMetrics;
import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.AnalyzeRiskUseCase;
import com.etf.risk.domain.port.in.ManagePortfolioUseCase;
import com.etf.risk.domain.port.in.RegisterUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Component
@RequiredArgsConstructor
public class RiskCommandHandler implements CommandHandler {

    private final RegisterUserUseCase registerUserUseCase;
    private final AnalyzeRiskUseCase analyzeRiskUseCase;
    private final ManagePortfolioUseCase managePortfolioUseCase;

    @Override
    public String getCommand() {
        return "/risk";
    }

    @Override
    public String getDescription() {
        return "리스크 분석";
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
                    분석할 포지션이 없습니다.

                    /add 명령어로 포지션을 추가하세요.
                    예: /add GOF 100 20.5""";
            }

            StringBuilder sb = new StringBuilder();
            sb.append("포트폴리오 리스크 분석\n");
            sb.append("═══════════════════\n\n");

            for (Position position : positions) {
                String symbol = position.getSymbol();
                RiskMetrics metrics = analyzeRiskUseCase.analyzeETFRisk(symbol);

                sb.append(String.format("[%s] %s\n", symbol, getRiskEmoji(metrics)));
                sb.append(String.format("종합 리스크: %s\n", metrics.overallRiskLevel().name()));
                sb.append("───────────────────\n");

                for (RiskMetrics.RiskFactor factor : metrics.riskFactors()) {
                    sb.append(String.format("  %s: %s\n", factor.category(), factor.level().name()));
                    sb.append(String.format("    %s\n", factor.message()));
                }

                if (metrics.requiresAction()) {
                    sb.append("\n  [!] 주의가 필요합니다\n");
                }
                sb.append("\n");
            }

            sb.append("═══════════════════\n");
            sb.append(getOverallAdvice(positions));

            return sb.toString();
        } catch (Exception e) {
            return "리스크 분석 중 오류가 발생했습니다: " + e.getMessage();
        }
    }

    private String getRiskEmoji(RiskMetrics metrics) {
        return switch (metrics.overallRiskLevel()) {
            case LOW -> "(안정)";
            case MEDIUM -> "(주의)";
            case HIGH -> "(위험)";
            case CRITICAL -> "(경고)";
        };
    }

    private String getOverallAdvice(List<Position> positions) {
        boolean hasCritical = false;
        boolean hasHigh = false;

        for (Position position : positions) {
            RiskMetrics metrics = analyzeRiskUseCase.analyzeETFRisk(position.getSymbol());
            if (metrics.overallRiskLevel().name().equals("CRITICAL")) {
                hasCritical = true;
            }
            if (metrics.overallRiskLevel().name().equals("HIGH")) {
                hasHigh = true;
            }
        }

        if (hasCritical) {
            return "즉각적인 포트폴리오 점검이 필요합니다.";
        } else if (hasHigh) {
            return "일부 포지션에 주의가 필요합니다.";
        } else {
            return "포트폴리오가 안정적입니다.";
        }
    }
}
