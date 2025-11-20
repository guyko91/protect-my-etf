package com.etf.risk.domain.port.in;

import com.etf.risk.domain.model.notification.NotificationMessage;

public interface SendNotificationUseCase {
    void sendNotification(NotificationMessage message);
    void sendDividendNotification(Long userId, String etfSymbol);
    void sendRiskAlert(Long userId, String etfSymbol);
}
