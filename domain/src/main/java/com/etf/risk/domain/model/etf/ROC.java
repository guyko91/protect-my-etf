package com.etf.risk.domain.model.etf;

import java.math.BigDecimal;

public record ROC(BigDecimal value) {
    private static final BigDecimal GOF_CRITICAL_THRESHOLD = new BigDecimal("50");
    private static final BigDecimal GOF_WARNING_THRESHOLD = new BigDecimal("30");
    private static final BigDecimal QQQI_CRITICAL_THRESHOLD = new BigDecimal("60");
    private static final BigDecimal QQQI_WARNING_THRESHOLD = new BigDecimal("40");

    public ROC {
        if (value == null) {
            throw new IllegalArgumentException("ROC 값은 null일 수 없습니다.");
        }
        if (value.compareTo(BigDecimal.ZERO) < 0 || value.compareTo(new BigDecimal("100")) > 0) {
            throw new IllegalArgumentException("ROC는 0~100% 사이여야 합니다.");
        }
    }

    public static ROC of(BigDecimal value) {
        return new ROC(value);
    }

    public static ROC of(String value) {
        return new ROC(new BigDecimal(value));
    }

    public boolean isCriticalForGOF() {
        return value.compareTo(GOF_CRITICAL_THRESHOLD) > 0;
    }

    public boolean isWarningForGOF() {
        return value.compareTo(GOF_WARNING_THRESHOLD) > 0
                && value.compareTo(GOF_CRITICAL_THRESHOLD) <= 0;
    }

    public boolean isCriticalForQQQI() {
        return value.compareTo(QQQI_CRITICAL_THRESHOLD) > 0;
    }

    public boolean isWarningForQQQI() {
        return value.compareTo(QQQI_WARNING_THRESHOLD) > 0
                && value.compareTo(QQQI_CRITICAL_THRESHOLD) <= 0;
    }

    @Override
    public String toString() {
        return value.toPlainString() + "%";
    }
}
