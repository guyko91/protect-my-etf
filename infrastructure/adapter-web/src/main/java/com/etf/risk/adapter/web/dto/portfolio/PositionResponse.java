package com.etf.risk.adapter.web.dto.portfolio;

import com.etf.risk.domain.model.portfolio.Position;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class PositionResponse {

    private final Long id;
    private final String symbol;
    private final Integer quantity;
    private final BigDecimal averagePrice;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    private PositionResponse(Long id, String symbol, Integer quantity, BigDecimal averagePrice,
                            LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.id = id;
        this.symbol = symbol;
        this.quantity = quantity;
        this.averagePrice = averagePrice;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PositionResponse from(Position position) {
        return new PositionResponse(
            position.getId(),
            position.getSymbol(),
            position.getQuantity(),
            position.getAveragePrice().getAmount(),
            position.getCreatedAt(),
            position.getUpdatedAt()
        );
    }
}
