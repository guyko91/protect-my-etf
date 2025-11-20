package com.etf.risk.domain.port.out;

import com.etf.risk.domain.model.notification.NotificationMessage;

public interface NotificationPort {
    void send(NotificationMessage message);
    boolean isAvailable();
}
