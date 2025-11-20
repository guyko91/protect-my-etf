package com.etf.risk.domain.model.etf;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.risk.RiskLevel;
import com.etf.risk.domain.model.risk.RiskMetrics;

import java.math.BigDecimal;
import java.util.Set;

public class QQQI extends ETF {
    private static final String SYMBOL = "QQQI";
    private static final String NAME = "NEOS Nasdaq 100 High Income ETF";
    private static final Set<ETFType> QQQI_TYPES = Set.of(
        ETFType.COVERED_CALL,
        ETFType.DIVIDEND,
        ETFType.INDEX
    );

    private ROC roc;
    private BigDecimal nasdaqTrend;
    private Money previousMonthDividend;

    private QQQI(ETFSnapshot snapshot, ROC roc, BigDecimal nasdaqTrend,
                 Money previousMonthDividend) {
        super(SYMBOL, NAME, QQQI_TYPES, snapshot);
        this.roc = roc;
        this.nasdaqTrend = nasdaqTrend;
        this.previousMonthDividend = previousMonthDividend;
    }

    public static QQQI create(ETFSnapshot snapshot, ROC roc, BigDecimal nasdaqTrend,
                              Money previousMonthDividend) {
        if (!snapshot.symbol().equals(SYMBOL)) {
            throw new IllegalArgumentException("QQQI가 아닌 심볼입니다: " + snapshot.symbol());
        }
        return new QQQI(snapshot, roc, nasdaqTrend, previousMonthDividend);
    }

    @Override
    public RiskMetrics analyzeRisk() {
        RiskMetrics.Builder builder = RiskMetrics.builder(SYMBOL);

        analyzeROC(builder);
        analyzeNasdaqTrend(builder);
        analyzeDividendSustainability(builder);

        return builder.build();
    }

    private void analyzeROC(RiskMetrics.Builder builder) {
        if (roc == null) {
            builder.addRiskFactor("ROC", RiskLevel.MEDIUM, "ROC 정보 없음");
            return;
        }

        if (roc.isCriticalForQQQI()) {
            builder.addRiskFactor("ROC", RiskLevel.HIGH,
                String.format("ROC 60%% 초과 (%s) - 구조 확인 필요", roc));
        } else if (roc.isWarningForQQQI()) {
            builder.addRiskFactor("ROC", RiskLevel.MEDIUM,
                String.format("ROC 40~60%% 범위 (%s) - 주의 필요", roc));
        } else {
            builder.addRiskFactor("ROC", RiskLevel.LOW,
                String.format("ROC 40%% 미만 (%s) - 정상", roc));
        }
    }

    private void analyzeNasdaqTrend(RiskMetrics.Builder builder) {
        if (nasdaqTrend == null) {
            builder.addRiskFactor("나스닥 추세", RiskLevel.LOW, "나스닥 추세 정보 없음");
            return;
        }

        if (nasdaqTrend.compareTo(BigDecimal.ZERO) < 0) {
            builder.addRiskFactor("나스닥 추세", RiskLevel.MEDIUM,
                String.format("나스닥100 하락 (%.2f%%) - 옵션 수익 감소 가능", nasdaqTrend));
        } else if (nasdaqTrend.compareTo(BigDecimal.ZERO) == 0) {
            builder.addRiskFactor("나스닥 추세", RiskLevel.LOW,
                "나스닥100 보합 - 안정");
        } else {
            builder.addRiskFactor("나스닥 추세", RiskLevel.LOW,
                String.format("나스닥100 상승 (+%.2f%%) - 양호", nasdaqTrend));
        }
    }

    private void analyzeDividendSustainability(RiskMetrics.Builder builder) {
        if (previousMonthDividend == null) {
            builder.addRiskFactor("배당 지속성", RiskLevel.LOW, "배당 이력 데이터 부족");
            return;
        }

        builder.addRiskFactor("배당 지속성", RiskLevel.LOW,
            String.format("전월 배당: %s", previousMonthDividend));
    }

    public void updateROC(ROC roc) {
        this.roc = roc;
    }

    public void updateNasdaqTrend(BigDecimal nasdaqTrend) {
        this.nasdaqTrend = nasdaqTrend;
    }

    public void updatePreviousMonthDividend(Money previousMonthDividend) {
        this.previousMonthDividend = previousMonthDividend;
    }

    public ROC roc() {
        return roc;
    }

    public BigDecimal nasdaqTrend() {
        return nasdaqTrend;
    }

    public Money previousMonthDividend() {
        return previousMonthDividend;
    }
}
