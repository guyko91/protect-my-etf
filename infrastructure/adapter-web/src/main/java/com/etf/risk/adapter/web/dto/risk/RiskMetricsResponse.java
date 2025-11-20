package com.etf.risk.adapter.web.dto.risk;

import com.etf.risk.domain.model.risk.RiskLevel;
import com.etf.risk.domain.model.risk.RiskMetrics;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
public class RiskMetricsResponse {

    private final String target;
    private final String overallRiskLevel;
    private final String overallRiskDescription;
    private final List<RiskFactorResponse> riskFactors;
    private final boolean requiresAction;
    private final boolean stable;

    private RiskMetricsResponse(String target, String overallRiskLevel, String overallRiskDescription,
                               List<RiskFactorResponse> riskFactors, boolean requiresAction, boolean stable) {
        this.target = target;
        this.overallRiskLevel = overallRiskLevel;
        this.overallRiskDescription = overallRiskDescription;
        this.riskFactors = riskFactors;
        this.requiresAction = requiresAction;
        this.stable = stable;
    }

    public static RiskMetricsResponse from(RiskMetrics metrics) {
        RiskLevel level = metrics.overallRiskLevel();
        List<RiskFactorResponse> factors = metrics.riskFactors().stream()
            .map(RiskFactorResponse::from)
            .collect(Collectors.toList());

        return new RiskMetricsResponse(
            metrics.etfSymbol(),
            level.getDisplayName(),
            level.getDescription(),
            factors,
            metrics.requiresAction(),
            metrics.isStable()
        );
    }

    @Getter
    public static class RiskFactorResponse {
        private final String category;
        private final String level;
        private final String message;

        private RiskFactorResponse(String category, String level, String message) {
            this.category = category;
            this.level = level;
            this.message = message;
        }

        public static RiskFactorResponse from(RiskMetrics.RiskFactor factor) {
            return new RiskFactorResponse(
                factor.category(),
                factor.level().getDisplayName(),
                factor.message()
            );
        }
    }
}
