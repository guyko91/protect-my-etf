package com.etf.risk.domain.model.etf;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.risk.RiskLevel;
import com.etf.risk.domain.model.risk.RiskMetrics;

import java.util.Set;

public class GOF extends ETF {
    private static final String SYMBOL = "GOF";
    private static final String NAME = "Guggenheim Strategic Opportunities Fund";
    private static final Set<ETFType> GOF_TYPES = Set.of(
        ETFType.CEF,
        ETFType.LEVERAGED,
        ETFType.DIVIDEND
    );

    private Premium premium;
    private Leverage leverage;
    private ROC roc;
    private Money previousMonthDividend;

    private GOF(ETFSnapshot snapshot, Premium premium, Leverage leverage,
                ROC roc, Money previousMonthDividend) {
        super(SYMBOL, NAME, GOF_TYPES, snapshot);
        this.premium = premium;
        this.leverage = leverage;
        this.roc = roc;
        this.previousMonthDividend = previousMonthDividend;
    }

    public static GOF create(ETFSnapshot snapshot, Premium premium, Leverage leverage,
                             ROC roc, Money previousMonthDividend) {
        if (!snapshot.symbol().equals(SYMBOL)) {
            throw new IllegalArgumentException("GOF가 아닌 심볼입니다: " + snapshot.symbol());
        }
        return new GOF(snapshot, premium, leverage, roc, previousMonthDividend);
    }

    @Override
    public RiskMetrics analyzeRisk() {
        RiskMetrics.Builder builder = RiskMetrics.builder(SYMBOL);

        analyzePremium(builder);
        analyzeLeverage(builder);
        analyzeROC(builder);
        analyzeDividendSustainability(builder);

        return builder.build();
    }

    private void analyzePremium(RiskMetrics.Builder builder) {
        if (premium == null) {
            builder.addRiskFactor("프리미엄/할인율", RiskLevel.MEDIUM, "프리미엄 정보 없음");
            return;
        }

        if (premium.isHighRisk()) {
            builder.addRiskFactor("프리미엄/할인율", RiskLevel.HIGH,
                String.format("프리미엄이 15%% 초과 (%s) - 신규 매수 금지 권장", premium));
        } else if (premium.isMediumRisk()) {
            builder.addRiskFactor("프리미엄/할인율", RiskLevel.MEDIUM,
                String.format("프리미엄이 10~15%% 범위 (%s) - 주의 필요", premium));
        } else {
            builder.addRiskFactor("프리미엄/할인율", RiskLevel.LOW,
                String.format("프리미엄이 10%% 미만 (%s) - 안정", premium));
        }
    }

    private void analyzeLeverage(RiskMetrics.Builder builder) {
        if (leverage == null) {
            builder.addRiskFactor("레버리지", RiskLevel.MEDIUM, "레버리지 정보 없음");
            return;
        }

        if (leverage.isIncreasing()) {
            builder.addRiskFactor("레버리지", RiskLevel.MEDIUM,
                String.format("레버리지 증가 (%s) - 리스크 상승", leverage));
        } else if (leverage.isDecreasing()) {
            builder.addRiskFactor("레버리지", RiskLevel.LOW,
                String.format("레버리지 감소 (%s) - 리스크 하락", leverage));
        } else {
            builder.addRiskFactor("레버리지", RiskLevel.LOW,
                String.format("레버리지 안정 (%s)", leverage));
        }
    }

    private void analyzeROC(RiskMetrics.Builder builder) {
        if (roc == null) {
            builder.addRiskFactor("ROC", RiskLevel.MEDIUM, "ROC 정보 없음");
            return;
        }

        if (roc.isCriticalForGOF()) {
            builder.addRiskFactor("ROC", RiskLevel.CRITICAL,
                String.format("ROC 50%% 초과 (%s) - NAV 잠식 위험", roc));
        } else if (roc.isWarningForGOF()) {
            builder.addRiskFactor("ROC", RiskLevel.MEDIUM,
                String.format("ROC 30~50%% 범위 (%s) - 주의 필요", roc));
        } else {
            builder.addRiskFactor("ROC", RiskLevel.LOW,
                String.format("ROC 30%% 미만 (%s) - 정상", roc));
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

    public void updatePremium(Premium premium) {
        this.premium = premium;
    }

    public void updateLeverage(Leverage leverage) {
        this.leverage = leverage;
    }

    public void updateROC(ROC roc) {
        this.roc = roc;
    }

    public void updatePreviousMonthDividend(Money previousMonthDividend) {
        this.previousMonthDividend = previousMonthDividend;
    }

    public Premium premium() {
        return premium;
    }

    public Leverage leverage() {
        return leverage;
    }

    public ROC roc() {
        return roc;
    }

    public Money previousMonthDividend() {
        return previousMonthDividend;
    }
}
