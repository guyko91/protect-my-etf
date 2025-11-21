package com.etf.risk.adapter.telegram;

import com.etf.risk.adapter.telegram.command.CommandHandler;
import com.etf.risk.adapter.telegram.config.TelegramBotProperties;
import com.etf.risk.domain.model.notification.NotificationMessage;
import com.etf.risk.domain.port.out.NotificationPort;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class TelegramBotAdapter extends TelegramLongPollingBot implements NotificationPort {

    private static final Logger log = LoggerFactory.getLogger(TelegramBotAdapter.class);

    private final TelegramBotProperties properties;
    private final Map<String, CommandHandler> commandHandlers;

    public TelegramBotAdapter(TelegramBotProperties properties, List<CommandHandler> handlers) {
        super(properties.getToken());
        this.properties = properties;
        this.commandHandlers = handlers.stream()
                .collect(Collectors.toMap(CommandHandler::getCommand, Function.identity()));
    }

    @PostConstruct
    public void init() {
        log.info("Telegram Bot initialized: {}", properties.getUsername());
        log.info("Registered commands: {}", commandHandlers.keySet());
    }

    @Override
    public String getBotUsername() {
        return properties.getUsername();
    }

    @Override
    public void onUpdateReceived(Update update) {
        if (!update.hasMessage() || !update.getMessage().hasText()) {
            return;
        }

        String messageText = update.getMessage().getText();
        Long chatId = update.getMessage().getChatId();

        log.debug("Received message from {}: {}", chatId, messageText);

        String response = processMessage(update, messageText);
        sendTextMessage(chatId, response);
    }

    private String processMessage(Update update, String messageText) {
        String command = extractCommand(messageText);
        CommandHandler handler = commandHandlers.get(command);

        if (handler != null) {
            try {
                return handler.handle(update);
            } catch (Exception e) {
                log.error("Error handling command {}: {}", command, e.getMessage(), e);
                return "명령 처리 중 오류가 발생했습니다. 잠시 후 다시 시도해주세요.";
            }
        }

        return "알 수 없는 명령어입니다. /help 명령어로 사용 가능한 명령어를 확인하세요.";
    }

    private String extractCommand(String messageText) {
        String[] parts = messageText.split("\\s+");
        return parts[0].toLowerCase();
    }

    private void sendTextMessage(Long chatId, String text) {
        SendMessage message = new SendMessage();
        message.setChatId(chatId.toString());
        message.setText(text);
        message.enableMarkdown(true);

        try {
            execute(message);
            log.debug("Message sent to {}", chatId);
        } catch (TelegramApiException e) {
            log.error("Failed to send message to {}: {}", chatId, e.getMessage(), e);
        }
    }

    @Override
    public void send(NotificationMessage message) {
        Long chatId = message.chatId().value();
        String formattedMessage = message.formatForTelegram();

        SendMessage sendMessage = new SendMessage();
        sendMessage.setChatId(chatId.toString());
        sendMessage.setText(formattedMessage);
        sendMessage.enableMarkdown(true);

        try {
            execute(sendMessage);
            log.info("Notification sent to {}: {}", chatId, message.title());
        } catch (TelegramApiException e) {
            log.error("Failed to send notification to {}: {}", chatId, e.getMessage(), e);
            throw new RuntimeException("텔레그램 메시지 전송 실패", e);
        }
    }

    @Override
    public boolean isAvailable() {
        return properties.getToken() != null && !properties.getToken().isBlank();
    }
}
