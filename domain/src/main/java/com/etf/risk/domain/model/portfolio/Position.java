package com.etf.risk.domain.model.portfolio;

import com.etf.risk.domain.exception.InsufficientQuantityException;
import com.etf.risk.domain.exception.InvalidQuantityException;
import com.etf.risk.domain.model.common.Money;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

public class Position {
    private Long id;
    private final String symbol;
    private int quantity;
    private Money averagePrice;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private Position(Long id, String symbol, int quantity, Money averagePrice, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Position create(String symbol, int quantity, Money averagePrice) {
        if (quantity <= 0) {
            throw new InvalidQuantityException("수량은 0보다 커야 합니다.");
        }
        if (averagePrice == null || !averagePrice.isPositive()) {
            throw new IllegalArgumentException("평균 매수가는 0보다 커야 합니다.");
        }

        LocalDateTime now = LocalDateTime.now();
        return new Position(null, symbol, quantity, averagePrice, now, now);
    }

    public void addQuantity(int additionalQuantity, Money purchasePrice) {
        if (additionalQuantity <= 0) {
            throw new InvalidQuantityException("추가 수량은 0보다 커야 합니다.");
        }
        if (purchasePrice == null || !purchasePrice.isPositive()) {
            throw new IllegalArgumentException("매수가는 0보다 커야 합니다.");
        }

        Money currentTotalValue = averagePrice.multiply(quantity);
        Money additionalValue = purchasePrice.multiply(additionalQuantity);
        Money newTotalValue = currentTotalValue.add(additionalValue);

        this.quantity += additionalQuantity;
        this.averagePrice = newTotalValue.divide(BigDecimal.valueOf(quantity));
        this.updatedAt = LocalDateTime.now();
    }

    public void reduceQuantity(int quantityToSell) {
        if (quantityToSell <= 0) {
            throw new InvalidQuantityException("매도 수량은 0보다 커야 합니다.");
        }
        if (quantityToSell > quantity) {
            throw new InsufficientQuantityException("보유 수량보다 많이 매도할 수 없습니다.");
        }

        this.quantity -= quantityToSell;
        this.updatedAt = LocalDateTime.now();
    }

    public Money calculateValue(Money currentPrice) {
        return currentPrice.multiply(quantity);
    }

    public BigDecimal calculateProfitLossRate(Money currentPrice) {
        Money diff = currentPrice.subtract(averagePrice);
        return diff.divide(averagePrice)
            .multiply(BigDecimal.valueOf(100))
            .setScale(2, RoundingMode.HALF_UP);
    }

    public Money calculateExpectedDividend(Money dividendPerShare) {
        return dividendPerShare.multiply(quantity);
    }

    public Long getId() {
        return id;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getQuantity() {
        return quantity;
    }

    public Money getAveragePrice() {
        return averagePrice;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
