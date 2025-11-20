package com.etf.risk.domain.model.risk;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class RiskMetrics {
    private final String etfSymbol;
    private final RiskLevel overallRiskLevel;
    private final List<RiskFactor> riskFactors;

    private RiskMetrics(String etfSymbol, RiskLevel overallRiskLevel, List<RiskFactor> riskFactors) {
        this.etfSymbol = etfSymbol;
        this.overallRiskLevel = overallRiskLevel;
        this.riskFactors = new ArrayList<>(riskFactors);
    }

    public static Builder builder(String etfSymbol) {
        return new Builder(etfSymbol);
    }

    public boolean requiresAction() {
        return overallRiskLevel.ordinal() >= RiskLevel.HIGH.ordinal();
    }

    public boolean isStable() {
        return overallRiskLevel == RiskLevel.LOW;
    }

    public String etfSymbol() {
        return etfSymbol;
    }

    public RiskLevel overallRiskLevel() {
        return overallRiskLevel;
    }

    public List<RiskFactor> riskFactors() {
        return Collections.unmodifiableList(riskFactors);
    }

    public static class Builder {
        private final String etfSymbol;
        private final List<RiskFactor> riskFactors = new ArrayList<>();

        private Builder(String etfSymbol) {
            this.etfSymbol = etfSymbol;
        }

        public Builder addRiskFactor(String category, RiskLevel level, String message) {
            riskFactors.add(new RiskFactor(category, level, message));
            return this;
        }

        public RiskMetrics build() {
            RiskLevel maxLevel = riskFactors.stream()
                .map(RiskFactor::level)
                .max(RiskLevel::compareTo)
                .orElse(RiskLevel.LOW);

            return new RiskMetrics(etfSymbol, maxLevel, riskFactors);
        }
    }

    public record RiskFactor(String category, RiskLevel level, String message) {
        public boolean isHighRisk() {
            return level.ordinal() >= RiskLevel.HIGH.ordinal();
        }
    }
}
