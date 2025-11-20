package com.etf.risk.adapter.persistence.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record UserPortfolioVO(
    Long id,
    Long userId,
    String etfSymbol,
    Integer quantity,
    BigDecimal averagePrice,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
}
