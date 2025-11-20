package com.etf.risk.domain.port.in;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.portfolio.Position;

import java.util.List;

public interface ManagePortfolioUseCase {
    void addPosition(Long userId, String etfSymbol, int quantity, Money averagePrice);
    void addToPosition(Long userId, String etfSymbol, int additionalQuantity, Money purchasePrice);
    void reducePosition(Long userId, String etfSymbol, int quantityToSell);
    void removePosition(Long userId, String etfSymbol);
    List<Position> getUserPositions(Long userId);
    Position getUserPosition(Long userId, String etfSymbol);
}
