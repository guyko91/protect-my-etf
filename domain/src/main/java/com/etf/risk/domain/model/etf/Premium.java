package com.etf.risk.domain.model.etf;

import java.math.BigDecimal;

public record Premium(BigDecimal value) {
    private static final BigDecimal HIGH_RISK_THRESHOLD = new BigDecimal("15");
    private static final BigDecimal MEDIUM_RISK_THRESHOLD = new BigDecimal("10");

    public Premium {
        if (value == null) {
            throw new IllegalArgumentException("프리미엄 값은 null일 수 없습니다.");
        }
    }

    public static Premium of(BigDecimal value) {
        return new Premium(value);
    }

    public static Premium of(String value) {
        return new Premium(new BigDecimal(value));
    }

    public boolean isHighRisk() {
        return value.compareTo(HIGH_RISK_THRESHOLD) > 0;
    }

    public boolean isMediumRisk() {
        return value.compareTo(MEDIUM_RISK_THRESHOLD) >= 0
                && value.compareTo(HIGH_RISK_THRESHOLD) <= 0;
    }

    public boolean isLowRisk() {
        return value.compareTo(MEDIUM_RISK_THRESHOLD) < 0;
    }

    @Override
    public String toString() {
        return value.toPlainString() + "%";
    }
}
