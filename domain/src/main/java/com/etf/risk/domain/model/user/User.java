package com.etf.risk.domain.model.user;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.portfolio.Portfolio;
import com.etf.risk.domain.model.portfolio.Position;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public class User {
    private Long id;
    private final TelegramChatId telegramChatId;
    private final String telegramUsername;
    private final Portfolio portfolio;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private User(Long id, TelegramChatId telegramChatId, String telegramUsername,
                 Portfolio portfolio, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.telegramChatId = telegramChatId;
        this.telegramUsername = telegramUsername;
        this.portfolio = portfolio;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static User register(TelegramChatId chatId, String username) {
        LocalDateTime now = LocalDateTime.now();
        return new User(null, chatId, username, Portfolio.createEmpty(), now, now);
    }

    public static User reconstitute(Long id, TelegramChatId chatId, String username,
                                     Portfolio portfolio, LocalDateTime createdAt, LocalDateTime updatedAt) {
        return new User(id, chatId, username, portfolio, createdAt, updatedAt);
    }

    public void addPosition(String symbol, int quantity, Money averagePrice) {
        portfolio.addPosition(symbol, quantity, averagePrice);
        this.updatedAt = LocalDateTime.now();
    }

    public void addToPosition(String symbol, int additionalQuantity, Money purchasePrice) {
        portfolio.addToPosition(symbol, additionalQuantity, purchasePrice);
        this.updatedAt = LocalDateTime.now();
    }

    public void reducePosition(String symbol, int quantityToSell) {
        portfolio.removeFromPosition(symbol, quantityToSell);
        this.updatedAt = LocalDateTime.now();
    }

    public void removePosition(String symbol) {
        portfolio.removePosition(symbol);
        this.updatedAt = LocalDateTime.now();
    }

    public boolean hasPosition(String symbol) {
        return portfolio.hasPosition(symbol);
    }

    public BigDecimal calculatePortfolioWeight(String symbol, Map<String, Money> currentPrices) {
        return portfolio.calculateWeight(symbol, currentPrices);
    }

    public Money calculateTotalPortfolioValue(Map<String, Money> currentPrices) {
        return portfolio.calculateTotalValue(currentPrices);
    }

    public List<Position> getPositions() {
        return portfolio.getPositions();
    }

    public Position getPosition(String symbol) {
        return portfolio.getPosition(symbol);
    }

    public boolean hasEmptyPortfolio() {
        return portfolio.isEmpty();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public TelegramChatId getTelegramChatId() {
        return telegramChatId;
    }

    public String getTelegramUsername() {
        return telegramUsername;
    }

    public Portfolio getPortfolio() {
        return portfolio;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
