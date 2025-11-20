package com.etf.risk.adapter.scraper.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record QQQIDataDTO(
    BigDecimal recentDividend,
    BigDecimal rocPercentage,
    BigDecimal nasdaqTrend,
    LocalDate recordedDate
) {
}
