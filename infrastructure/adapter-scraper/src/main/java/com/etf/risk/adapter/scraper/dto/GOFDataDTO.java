package com.etf.risk.adapter.scraper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GOFDataDTO(
    LocalDate exDividendDate,
    LocalDate paymentDate,
    BigDecimal amountPerShare,
    BigDecimal rocPercentage,
    BigDecimal premiumDiscount,
    BigDecimal leverageRatio
) {
}
