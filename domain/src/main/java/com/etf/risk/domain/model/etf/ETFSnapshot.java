package com.etf.risk.domain.model.etf;

import com.etf.risk.domain.model.common.Money;

import java.time.LocalDate;

public record ETFSnapshot(
    String symbol,
    Money currentPrice,
    Money nav,
    LocalDate recordedDate
) {
    public ETFSnapshot {
        if (symbol == null || symbol.isBlank()) {
            throw new IllegalArgumentException("ETF 심볼은 필수입니다");
        }
        if (currentPrice == null) {
            throw new IllegalArgumentException("현재 가격은 필수입니다");
        }
        if (nav == null) {
            throw new IllegalArgumentException("NAV는 필수입니다");
        }
        if (recordedDate == null) {
            throw new IllegalArgumentException("기록 날짜는 필수입니다");
        }
    }

    public Money calculatePremiumOrDiscount() {
        return currentPrice.subtract(nav);
    }

    public boolean isTradingAtPremium() {
        return currentPrice.isGreaterThan(nav);
    }

    public boolean isTradingAtDiscount() {
        return currentPrice.isLessThan(nav);
    }
}
