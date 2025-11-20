package com.etf.risk.domain.model.portfolio;

import com.etf.risk.domain.exception.DuplicatePositionException;
import com.etf.risk.domain.exception.PositionNotFoundException;
import com.etf.risk.domain.model.common.Money;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Portfolio {
    private final List<Position> positions;

    private Portfolio(List<Position> positions) {
        this.positions = new ArrayList<>(positions);
    }

    public static Portfolio createEmpty() {
        return new Portfolio(new ArrayList<>());
    }

    public void addPosition(String symbol, int quantity, Money averagePrice) {
        if (hasPosition(symbol)) {
            throw new DuplicatePositionException("이미 보유 중인 ETF: " + symbol);
        }
        positions.add(Position.create(symbol, quantity, averagePrice));
    }

    public void addToPosition(String symbol, int additionalQuantity, Money purchasePrice) {
        Position position = findPositionOrThrow(symbol);
        position.addQuantity(additionalQuantity, purchasePrice);
    }

    public void removeFromPosition(String symbol, int quantityToSell) {
        Position position = findPositionOrThrow(symbol);
        position.reduceQuantity(quantityToSell);

        if (position.getQuantity() == 0) {
            positions.remove(position);
        }
    }

    public void removePosition(String symbol) {
        if (!positions.removeIf(p -> p.getSymbol().equals(symbol))) {
            throw new PositionNotFoundException("ETF를 보유하고 있지 않습니다: " + symbol);
        }
    }

    public boolean hasPosition(String symbol) {
        return positions.stream()
            .anyMatch(p -> p.getSymbol().equals(symbol));
    }

    public BigDecimal calculateWeight(String symbol, Map<String, Money> currentPrices) {
        Money totalValue = calculateTotalValue(currentPrices);
        if (totalValue.isZero()) {
            return BigDecimal.ZERO;
        }

        Position position = findPositionOrThrow(symbol);
        Money currentPrice = currentPrices.get(symbol);
        if (currentPrice == null) {
            throw new IllegalArgumentException("시세 정보가 없습니다: " + symbol);
        }

        Money positionValue = position.calculateValue(currentPrice);
        return positionValue.divide(totalValue)
            .multiply(BigDecimal.valueOf(100));
    }

    public Money calculateTotalValue(Map<String, Money> currentPrices) {
        return positions.stream()
            .map(p -> {
                Money currentPrice = currentPrices.get(p.getSymbol());
                if (currentPrice == null) {
                    throw new IllegalArgumentException("시세 정보가 없습니다: " + p.getSymbol());
                }
                return p.calculateValue(currentPrice);
            })
            .reduce(Money.ZERO, Money::add);
    }

    public List<Position> getPositions() {
        return Collections.unmodifiableList(positions);
    }

    public Position getPosition(String symbol) {
        return findPositionOrThrow(symbol);
    }

    public boolean isEmpty() {
        return positions.isEmpty();
    }

    public int getPositionCount() {
        return positions.size();
    }

    private Position findPositionOrThrow(String symbol) {
        return positions.stream()
            .filter(p -> p.getSymbol().equals(symbol))
            .findFirst()
            .orElseThrow(() -> new PositionNotFoundException("ETF를 보유하고 있지 않습니다: " + symbol));
    }
}
