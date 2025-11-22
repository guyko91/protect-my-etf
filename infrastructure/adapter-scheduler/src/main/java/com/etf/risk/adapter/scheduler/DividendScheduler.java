package com.etf.risk.adapter.scheduler;

import com.etf.risk.adapter.scheduler.config.SchedulerProperties;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.SendNotificationUseCase;
import com.etf.risk.domain.port.out.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

@Component
public class DividendScheduler {

    private static final Logger log = LoggerFactory.getLogger(DividendScheduler.class);

    private static final Set<String> SUPPORTED_ETFS = Set.of("GOF", "QQQI");

    private static final int GOF_PAYMENT_DAY = 31;
    private static final int QQQI_PAYMENT_DAY = 28;

    private final SchedulerProperties properties;
    private final UserRepository userRepository;
    private final SendNotificationUseCase sendNotificationUseCase;

    public DividendScheduler(SchedulerProperties properties,
                             UserRepository userRepository,
                             SendNotificationUseCase sendNotificationUseCase) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.sendNotificationUseCase = sendNotificationUseCase;
    }

    @Scheduled(cron = "${scheduler.dividend.cron}", zone = "${scheduler.dividend.zone}")
    public void processDividendNotifications() {
        if (!properties.isEnabled()) {
            log.debug("Scheduler is disabled, skipping dividend notifications");
            return;
        }

        log.info("Starting dividend notification process");
        LocalDate today = LocalDate.now();

        for (String etfSymbol : SUPPORTED_ETFS) {
            if (isPaymentDay(etfSymbol, today)) {
                processETFDividend(etfSymbol);
            }
        }

        log.info("Dividend notification process completed");
    }

    private boolean isPaymentDay(String etfSymbol, LocalDate date) {
        int dayOfMonth = date.getDayOfMonth();
        int lastDayOfMonth = date.lengthOfMonth();

        return switch (etfSymbol) {
            case "GOF" -> dayOfMonth == lastDayOfMonth || dayOfMonth == GOF_PAYMENT_DAY;
            case "QQQI" -> dayOfMonth == QQQI_PAYMENT_DAY || dayOfMonth == lastDayOfMonth;
            default -> false;
        };
    }

    private void processETFDividend(String etfSymbol) {
        log.info("Processing dividend notifications for ETF: {}", etfSymbol);

        List<User> users = userRepository.findUsersWithETF(etfSymbol);

        if (users.isEmpty()) {
            log.info("No users holding {} found", etfSymbol);
            return;
        }

        log.info("Found {} users holding {}", users.size(), etfSymbol);

        for (User user : users) {
            try {
                sendNotificationUseCase.sendDividendNotification(user.getId(), etfSymbol);
                sendNotificationUseCase.sendRiskAlert(user.getId(), etfSymbol);
                log.debug("Notifications sent to user {} for {}", user.getId(), etfSymbol);
            } catch (Exception e) {
                log.error("Failed to send notification to user {} for {}: {}",
                        user.getId(), etfSymbol, e.getMessage(), e);
            }
        }

        log.info("Completed dividend notifications for {}", etfSymbol);
    }

    public void triggerManually(String etfSymbol) {
        if (!SUPPORTED_ETFS.contains(etfSymbol)) {
            throw new IllegalArgumentException("Unsupported ETF: " + etfSymbol);
        }

        log.info("Manual trigger for ETF: {}", etfSymbol);
        processETFDividend(etfSymbol);
    }

    public void triggerAllManually() {
        log.info("Manual trigger for all ETFs");
        for (String etfSymbol : SUPPORTED_ETFS) {
            processETFDividend(etfSymbol);
        }
    }
}
