package com.etf.risk.domain.model.etf;

import java.math.BigDecimal;

public record Leverage(BigDecimal current, BigDecimal previous) {

    public Leverage {
        if (current == null) {
            throw new IllegalArgumentException("현재 레버리지 값은 null일 수 없습니다.");
        }
        if (current.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("레버리지는 0 이상이어야 합니다.");
        }
    }

    public static Leverage of(BigDecimal current, BigDecimal previous) {
        return new Leverage(current, previous);
    }

    public static Leverage of(String current, String previous) {
        return new Leverage(
            new BigDecimal(current),
            previous != null ? new BigDecimal(previous) : null
        );
    }

    public boolean isIncreasing() {
        if (previous == null) {
            return false;
        }
        return current.compareTo(previous) > 0;
    }

    public boolean isDecreasing() {
        if (previous == null) {
            return false;
        }
        return current.compareTo(previous) < 0;
    }

    public boolean isStable() {
        if (previous == null) {
            return true;
        }
        return current.compareTo(previous) == 0;
    }

    public BigDecimal getChangeRate() {
        if (previous == null || previous.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return current.subtract(previous)
            .divide(previous, 4, java.math.RoundingMode.HALF_UP)
            .multiply(new BigDecimal("100"));
    }

    @Override
    public String toString() {
        if (previous == null) {
            return current.toPlainString() + "%";
        }
        return String.format("%s%% (이전: %s%%)", current.toPlainString(), previous.toPlainString());
    }
}
