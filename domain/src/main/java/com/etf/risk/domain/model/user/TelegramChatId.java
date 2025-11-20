package com.etf.risk.domain.model.user;

public record TelegramChatId(Long value) {

    public TelegramChatId {
        if (value == null) {
            throw new IllegalArgumentException("텔레그램 Chat ID는 null일 수 없습니다.");
        }
        if (value == 0) {
            throw new IllegalArgumentException("텔레그램 Chat ID는 0일 수 없습니다.");
        }
    }

    public static TelegramChatId of(Long value) {
        return new TelegramChatId(value);
    }

    public static TelegramChatId of(String value) {
        try {
            return new TelegramChatId(Long.parseLong(value));
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("유효하지 않은 텔레그램 Chat ID 형식입니다: " + value, e);
        }
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
