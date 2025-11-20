package com.etf.risk.domain.port.in;

import com.etf.risk.domain.model.risk.RiskMetrics;

public interface AnalyzeRiskUseCase {
    RiskMetrics analyzeETFRisk(String etfSymbol);
    RiskMetrics analyzeUserPortfolioRisk(Long userId);
}
