package com.etf.risk.application.service;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.dividend.Dividend;
import com.etf.risk.domain.model.notification.NotificationMessage;
import com.etf.risk.domain.model.notification.NotificationPriority;
import com.etf.risk.domain.model.portfolio.Position;
import com.etf.risk.domain.model.risk.RiskLevel;
import com.etf.risk.domain.model.risk.RiskMetrics;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.AnalyzeRiskUseCase;
import com.etf.risk.domain.port.in.SendNotificationUseCase;
import com.etf.risk.domain.port.out.DividendRepository;
import com.etf.risk.domain.port.out.NotificationPort;
import com.etf.risk.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class NotificationService implements SendNotificationUseCase {

    private final NotificationPort notificationPort;
    private final UserRepository userRepository;
    private final DividendRepository dividendRepository;
    private final AnalyzeRiskUseCase analyzeRiskUseCase;

    public NotificationService(NotificationPort notificationPort,
                               UserRepository userRepository,
                               DividendRepository dividendRepository,
                               AnalyzeRiskUseCase analyzeRiskUseCase) {
        this.notificationPort = notificationPort;
        this.userRepository = userRepository;
        this.dividendRepository = dividendRepository;
        this.analyzeRiskUseCase = analyzeRiskUseCase;
    }

    @Override
    public void sendNotification(NotificationMessage message) {
        if (!notificationPort.isAvailable()) {
            throw new IllegalStateException("알림 서비스를 사용할 수 없습니다");
        }
        notificationPort.send(message);
    }

    @Override
    public void sendDividendNotification(Long userId, String etfSymbol) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        if (!user.hasPosition(etfSymbol)) {
            return;
        }

        Position position = user.getPosition(etfSymbol);
        Dividend latestDividend = dividendRepository.findLatest(etfSymbol)
            .orElse(null);

        String content = buildDividendNotificationContent(position, latestDividend);
        NotificationPriority priority = latestDividend != null && latestDividend.hasROC()
            ? NotificationPriority.NORMAL
            : NotificationPriority.LOW;

        NotificationMessage message = NotificationMessage.create(
            user.getTelegramChatId(),
            etfSymbol + " 배당 알림",
            content,
            priority
        );

        sendNotification(message);
    }

    @Override
    public void sendRiskAlert(Long userId, String etfSymbol) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        RiskMetrics riskMetrics = analyzeRiskUseCase.analyzeETFRisk(etfSymbol);

        String content = buildRiskAlertContent(etfSymbol, riskMetrics);
        NotificationPriority priority = determinePriority(riskMetrics.overallRiskLevel());

        NotificationMessage message = NotificationMessage.create(
            user.getTelegramChatId(),
            etfSymbol + " 리스크 알림",
            content,
            priority
        );

        sendNotification(message);
    }

    private String buildDividendNotificationContent(Position position, Dividend dividend) {
        StringBuilder sb = new StringBuilder();
        sb.append("보유 수량: ").append(position.getQuantity()).append("주\n");
        sb.append("평균 매수가: ").append(position.getAveragePrice()).append("\n\n");

        if (dividend != null) {
            Money expectedDividend = dividend.calculateTotalDividend(position.getQuantity());
            sb.append("주당 배당금: ").append(dividend.amountPerShare()).append("\n");
            sb.append("예상 배당금: ").append(expectedDividend).append("\n");
            sb.append("배당 지급일: ").append(dividend.paymentDate()).append("\n");

            if (dividend.hasROC()) {
                sb.append("\nROC: ").append(dividend.rocPercentage()).append("\n");
            }
        } else {
            sb.append("배당 정보가 없습니다.");
        }

        return sb.toString();
    }

    private String buildRiskAlertContent(String etfSymbol, RiskMetrics riskMetrics) {
        StringBuilder sb = new StringBuilder();
        sb.append("종합 리스크: ").append(riskMetrics.overallRiskLevel().getDisplayName())
          .append(" (").append(riskMetrics.overallRiskLevel().getDescription()).append(")\n\n");

        sb.append("상세 분석:\n");
        riskMetrics.riskFactors().forEach(factor -> {
            sb.append("- [").append(factor.level().getDisplayName()).append("] ")
              .append(factor.category()).append(": ")
              .append(factor.message()).append("\n");
        });

        if (riskMetrics.requiresAction()) {
            sb.append("\n주의: 즉시 조치가 필요할 수 있습니다.");
        }

        return sb.toString();
    }

    private NotificationPriority determinePriority(RiskLevel riskLevel) {
        return switch (riskLevel) {
            case LOW -> NotificationPriority.LOW;
            case MEDIUM -> NotificationPriority.NORMAL;
            case HIGH -> NotificationPriority.HIGH;
            case CRITICAL -> NotificationPriority.URGENT;
        };
    }
}
