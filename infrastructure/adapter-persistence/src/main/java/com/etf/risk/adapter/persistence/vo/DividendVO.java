package com.etf.risk.adapter.persistence.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record DividendVO(
    Long id,
    String etfSymbol,
    LocalDate exDividendDate,
    LocalDate paymentDate,
    BigDecimal amountPerShare,
    BigDecimal rocPercentage,
    LocalDateTime createdAt
) {
}
