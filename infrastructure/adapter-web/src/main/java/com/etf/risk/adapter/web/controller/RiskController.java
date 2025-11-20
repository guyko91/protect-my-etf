package com.etf.risk.adapter.web.controller;

import com.etf.risk.adapter.web.common.ApiResponse;
import com.etf.risk.adapter.web.dto.risk.RiskMetricsResponse;
import com.etf.risk.domain.model.risk.RiskMetrics;
import com.etf.risk.domain.port.in.AnalyzeRiskUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/risk")
@RequiredArgsConstructor
public class RiskController {

    private final AnalyzeRiskUseCase analyzeRiskUseCase;

    @GetMapping("/etf/{symbol}")
    public ApiResponse<RiskMetricsResponse> analyzeETFRisk(@PathVariable String symbol) {
        RiskMetrics metrics = analyzeRiskUseCase.analyzeETFRisk(symbol);
        return ApiResponse.success(RiskMetricsResponse.from(metrics));
    }

    @GetMapping("/portfolio/{userId}")
    public ApiResponse<RiskMetricsResponse> analyzePortfolioRisk(@PathVariable Long userId) {
        RiskMetrics metrics = analyzeRiskUseCase.analyzeUserPortfolioRisk(userId);
        return ApiResponse.success(RiskMetricsResponse.from(metrics));
    }
}
