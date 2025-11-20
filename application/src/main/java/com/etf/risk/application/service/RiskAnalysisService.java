package com.etf.risk.application.service;

import com.etf.risk.domain.model.etf.ETF;
import com.etf.risk.domain.model.portfolio.Position;
import com.etf.risk.domain.model.risk.RiskMetrics;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.AnalyzeRiskUseCase;
import com.etf.risk.domain.port.out.ETFDataPort;
import com.etf.risk.domain.port.out.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class RiskAnalysisService implements AnalyzeRiskUseCase {

    private final ETFDataPort etfDataPort;
    private final UserRepository userRepository;

    public RiskAnalysisService(ETFDataPort etfDataPort, UserRepository userRepository) {
        this.etfDataPort = etfDataPort;
        this.userRepository = userRepository;
    }

    @Override
    public RiskMetrics analyzeETFRisk(String etfSymbol) {
        ETF etf = etfDataPort.findETFBySymbol(etfSymbol)
            .orElseThrow(() -> new IllegalArgumentException("ETF를 찾을 수 없습니다: " + etfSymbol));

        return etf.analyzeRisk();
    }

    @Override
    public RiskMetrics analyzeUserPortfolioRisk(Long userId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));

        List<Position> positions = user.getPositions();
        if (positions.isEmpty()) {
            throw new IllegalStateException("포트폴리오가 비어있습니다");
        }

        RiskMetrics.Builder overallRiskBuilder = RiskMetrics.builder("PORTFOLIO_" + userId);

        for (Position position : positions) {
            RiskMetrics etfRisk = analyzeETFRisk(position.getSymbol());

            etfRisk.riskFactors().forEach(factor ->
                overallRiskBuilder.addRiskFactor(
                    position.getSymbol() + " - " + factor.category(),
                    factor.level(),
                    factor.message()
                )
            );
        }

        return overallRiskBuilder.build();
    }
}
