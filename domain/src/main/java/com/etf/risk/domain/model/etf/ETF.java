package com.etf.risk.domain.model.etf;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.risk.RiskMetrics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public abstract class ETF {
    protected final String symbol;
    protected final String name;
    protected final Set<ETFType> types;
    protected ETFSnapshot snapshot;

    protected ETF(String symbol, String name, Set<ETFType> types, ETFSnapshot snapshot) {
        this.symbol = symbol;
        this.name = name;
        this.types = new HashSet<>(types);
        this.snapshot = snapshot;
    }

    public abstract RiskMetrics analyzeRisk();

    public BigDecimal calculateYield(Money annualDividend) {
        if (snapshot == null || snapshot.currentPrice().isZero()) {
            return BigDecimal.ZERO;
        }

        BigDecimal price = snapshot.currentPrice().getAmount();
        return annualDividend.getAmount()
            .divide(price, 4, RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));
    }

    public void updateSnapshot(ETFSnapshot newSnapshot) {
        if (!newSnapshot.symbol().equals(this.symbol)) {
            throw new IllegalArgumentException("ETF 심볼이 일치하지 않습니다");
        }
        this.snapshot = newSnapshot;
    }

    public String symbol() {
        return symbol;
    }

    public String name() {
        return name;
    }

    public Set<ETFType> types() {
        return Collections.unmodifiableSet(types);
    }

    public boolean hasType(ETFType type) {
        return types.contains(type);
    }

    public ETFSnapshot snapshot() {
        return snapshot;
    }

    public Money currentPrice() {
        return snapshot != null ? snapshot.currentPrice() : Money.ZERO;
    }

    public Money nav() {
        return snapshot != null ? snapshot.nav() : Money.ZERO;
    }
}
