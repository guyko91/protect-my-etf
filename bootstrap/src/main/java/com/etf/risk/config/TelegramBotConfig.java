package com.etf.risk.config;

import com.etf.risk.adapter.telegram.TelegramBotAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import jakarta.annotation.PostConstruct;

@Configuration
public class TelegramBotConfig {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotConfig.class);

    private final TelegramBotAdapter telegramBotAdapter;

    public TelegramBotConfig(TelegramBotAdapter telegramBotAdapter) {
        this.telegramBotAdapter = telegramBotAdapter;
    }

    @PostConstruct
    public void registerBot() {
        try {
            TelegramBotsApi botsApi = new TelegramBotsApi(DefaultBotSession.class);
            botsApi.registerBot(telegramBotAdapter);
            log.info("Telegram bot registered successfully");
        } catch (TelegramApiException e) {
            log.error("Failed to register Telegram bot: {}", e.getMessage(), e);
        }
    }
}
