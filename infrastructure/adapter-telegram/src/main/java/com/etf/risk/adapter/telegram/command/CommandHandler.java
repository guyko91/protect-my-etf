package com.etf.risk.adapter.telegram.command;

import org.telegram.telegrambots.meta.api.objects.Update;

public interface CommandHandler {
    String getCommand();
    String getDescription();
    String handle(Update update);
}
