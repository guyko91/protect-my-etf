package com.etf.risk.adapter.persistence.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record RiskMetricsVO(
    Long id,
    String etfSymbol,
    LocalDate recordedDate,
    BigDecimal nav,
    BigDecimal currentPrice,
    BigDecimal premiumDiscount,
    BigDecimal leverageRatio,
    BigDecimal nasdaqTrend,
    LocalDateTime createdAt
) {
}
