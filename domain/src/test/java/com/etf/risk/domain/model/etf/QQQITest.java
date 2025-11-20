package com.etf.risk.domain.model.etf;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.risk.RiskLevel;
import com.etf.risk.domain.model.risk.RiskMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("QQQI 리스크 분석 테스트")
class QQQITest {

    @Test
    @DisplayName("QQQI 생성 - 안정적인 상태")
    void createQQQI_stableState() {
        // Given
        ETFSnapshot snapshot = new ETFSnapshot(
            "QQQI",
            Money.of("55.50"),
            Money.of("53.05"),
            LocalDate.now()
        );
        ROC roc = ROC.of("35.0");  // ROC 35% (정상)
        BigDecimal nasdaqTrend = new BigDecimal("3.5");  // +3.5% 상승

        // When
        QQQI qqqi = QQQI.create(snapshot, roc, nasdaqTrend, Money.of("0.6445"));

        // Then
        assertThat(qqqi.symbol()).isEqualTo("QQQI");
        assertThat(qqqi.currentPrice()).isEqualTo(Money.of("55.50"));
        assertThat(qqqi.roc()).isEqualTo(roc);
        assertThat(qqqi.nasdaqTrend()).isEqualByComparingTo(nasdaqTrend);
    }

    @Test
    @DisplayName("리스크 분석 - 모든 지표가 안정적인 경우 (LOW)")
    void analyzeRisk_allStable_returnsLow() {
        // Given: ROC 35%, 나스닥 상승
        QQQI qqqi = createQQQI(
            ROC.of("35.0"),
            new BigDecimal("2.5")  // +2.5% 상승
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(riskMetrics.isStable()).isTrue();
    }

    @Test
    @DisplayName("리스크 분석 - ROC 주의 구간 (40~60%)")
    void analyzeRisk_rocWarning_returnsMedium() {
        // Given: ROC 50%
        QQQI qqqi = createQQQI(
            ROC.of("50.0"),
            new BigDecimal("1.0")
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.MEDIUM);

        RiskMetrics.RiskFactor rocFactor = riskMetrics.riskFactors().stream()
            .filter(f -> f.category().contains("ROC"))
            .findFirst()
            .orElseThrow();

        assertThat(rocFactor.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(rocFactor.message()).contains("주의");
    }

    @Test
    @DisplayName("리스크 분석 - ROC 위험 구간 (>60%, 구조 확인 필요)")
    void analyzeRisk_rocCritical_returnsHigh() {
        // Given: ROC 75%
        QQQI qqqi = createQQQI(
            ROC.of("75.0"),
            new BigDecimal("0.5")
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(riskMetrics.requiresAction()).isTrue();

        RiskMetrics.RiskFactor rocFactor = riskMetrics.riskFactors().stream()
            .filter(f -> f.category().contains("ROC"))
            .findFirst()
            .orElseThrow();

        assertThat(rocFactor.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(rocFactor.message()).contains("구조 확인 필요");
    }

    @Test
    @DisplayName("리스크 분석 - 나스닥 하락 시 옵션 수익 감소 우려")
    void analyzeRisk_nasdaqDowntrend_returnsMedium() {
        // Given: ROC 정상, 나스닥 -2.5% 하락
        QQQI qqqi = createQQQI(
            ROC.of("30.0"),
            new BigDecimal("-2.5")  // 하락
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.MEDIUM);

        RiskMetrics.RiskFactor nasdaqFactor = riskMetrics.riskFactors().stream()
            .filter(f -> f.category().contains("나스닥"))
            .findFirst()
            .orElseThrow();

        assertThat(nasdaqFactor.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(nasdaqFactor.message()).contains("하락");
        assertThat(nasdaqFactor.message()).contains("옵션 수익 감소");
    }

    @Test
    @DisplayName("리스크 분석 - 나스닥 보합 시 안정")
    void analyzeRisk_nasdaqFlat_returnsLow() {
        // Given: 나스닥 보합 (0%)
        QQQI qqqi = createQQQI(
            ROC.of("30.0"),
            BigDecimal.ZERO
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then
        RiskMetrics.RiskFactor nasdaqFactor = riskMetrics.riskFactors().stream()
            .filter(f -> f.category().contains("나스닥"))
            .findFirst()
            .orElseThrow();

        assertThat(nasdaqFactor.level()).isEqualTo(RiskLevel.LOW);
        assertThat(nasdaqFactor.message()).contains("보합");
    }

    @Test
    @DisplayName("리스크 분석 - 나스닥 상승 시 긍정적")
    void analyzeRisk_nasdaqUptrend_returnsLow() {
        // Given: 나스닥 +5% 상승
        QQQI qqqi = createQQQI(
            ROC.of("30.0"),
            new BigDecimal("5.0")
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then
        RiskMetrics.RiskFactor nasdaqFactor = riskMetrics.riskFactors().stream()
            .filter(f -> f.category().contains("나스닥"))
            .findFirst()
            .orElseThrow();

        assertThat(nasdaqFactor.level()).isEqualTo(RiskLevel.LOW);
        assertThat(nasdaqFactor.message()).contains("상승");
        assertThat(nasdaqFactor.message()).contains("양호");
    }

    @Test
    @DisplayName("리스크 분석 - 복합 위험 (ROC 높음 + 나스닥 하락)")
    void analyzeRisk_multipleRisks_returnsHigh() {
        // Given: ROC 70%, 나스닥 -3% 하락
        QQQI qqqi = createQQQI(
            ROC.of("70.0"),
            new BigDecimal("-3.0")
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(riskMetrics.requiresAction()).isTrue();

        // 2개의 위험 요소
        long highRiskFactors = riskMetrics.riskFactors().stream()
            .filter(f -> f.level().ordinal() >= RiskLevel.MEDIUM.ordinal())
            .count();
        assertThat(highRiskFactors).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("실제 시나리오 - 2025년 QQQI 상태 (ROC 100%)")
    void realWorldScenario_2025QQQI() {
        // Given: 2025년 실제 QQQI 데이터
        ETFSnapshot snapshot = new ETFSnapshot(
            "QQQI",
            Money.of("55.50"),  // 현재가
            Money.of("53.05"),  // NAV
            LocalDate.of(2025, 11, 20)
        );

        QQQI qqqi = QQQI.create(
            snapshot,
            ROC.of("100.0"),    // ROC 100% (매우 높음, 하지만 NEOS 전략상 정상일 수 있음)
            new BigDecimal("5.2"),  // 나스닥 +5.2% 상승
            Money.of("0.6445")  // 월 배당 $0.6445
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then: ROC가 60% 초과이므로 HIGH
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.HIGH);

        // ROC: HIGH (100% > 60%)
        // 나스닥 추세: LOW (상승)
        assertThat(riskMetrics.riskFactors()).hasSize(3); // ROC + 나스닥 + 배당 지속성
    }

    @Test
    @DisplayName("실제 시나리오 - 이상적인 QQQI 투자 시점")
    void realWorldScenario_idealEntryPoint() {
        // Given: 이상적인 진입 시점
        // - ROC 낮음 (30% 이하)
        // - 나스닥 상승 추세
        QQQI qqqi = createQQQI(
            ROC.of("25.0"),
            new BigDecimal("8.5")  // 나스닥 강한 상승
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(riskMetrics.isStable()).isTrue();

        // 모든 요소가 LOW 레벨
        long lowRiskFactors = riskMetrics.riskFactors().stream()
            .filter(f -> f.level() == RiskLevel.LOW)
            .count();
        assertThat(lowRiskFactors).isEqualTo(3);
    }

    @Test
    @DisplayName("실제 시나리오 - 나스닥 조정장에서의 QQQI")
    void realWorldScenario_nasdaqCorrection() {
        // Given: 나스닥 조정장 (-10% 하락)
        QQQI qqqi = createQQQI(
            ROC.of("45.0"),  // ROC 약간 높음
            new BigDecimal("-10.0")  // 나스닥 큰 폭 하락
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then: 나스닥 하락으로 MEDIUM 이상
        assertThat(riskMetrics.overallRiskLevel().ordinal())
            .isGreaterThanOrEqualTo(RiskLevel.MEDIUM.ordinal());

        // 나스닥 하락과 ROC 주의 두 가지 요소
        long warningFactors = riskMetrics.riskFactors().stream()
            .filter(f -> f.level() == RiskLevel.MEDIUM)
            .count();
        assertThat(warningFactors).isGreaterThanOrEqualTo(2);
    }

    @Test
    @DisplayName("연 배당 수익률 계산 - 고배당 ETF")
    void calculateYield() {
        // Given: QQQI 현재가 $55.50
        ETFSnapshot snapshot = new ETFSnapshot(
            "QQQI",
            Money.of("55.50"),
            Money.of("53.05"),
            LocalDate.now()
        );
        QQQI qqqi = QQQI.create(snapshot, null, null, null);

        // When: 월 배당 $0.6445 × 12 = $7.734
        Money annualDividend = Money.of("0.6445").multiply(12);
        BigDecimal yieldRate = qqqi.calculateYield(annualDividend);

        // Then: 7.734 / 55.50 * 100 = 13.93%
        assertThat(yieldRate.doubleValue()).isCloseTo(13.93, within(0.01));
    }

    @Test
    @DisplayName("실제 시나리오 - QQQI 월배당 계산")
    void realWorldScenario_monthlyDividendCalculation() {
        // Given: QQQI 100주 보유
        int quantity = 100;
        Money dividendPerShare = Money.of("0.6445");

        // When: 월 배당금 계산
        Money monthlyDividend = dividendPerShare.multiply(quantity);

        // Then: $64.45
        assertThat(monthlyDividend).isEqualTo(Money.of("64.45"));

        // When: 연 배당금 계산
        Money annualDividend = monthlyDividend.multiply(12);

        // Then: $773.40
        assertThat(annualDividend).isEqualTo(Money.of("773.40"));
    }

    @Test
    @DisplayName("ROC/나스닥추세 업데이트")
    void updateRiskFactors() {
        // Given
        QQQI qqqi = createQQQI(
            ROC.of("30.0"),
            new BigDecimal("2.0")
        );

        // When
        qqqi.updateROC(ROC.of("55.0"));
        qqqi.updateNasdaqTrend(new BigDecimal("-3.5"));

        // Then
        assertThat(qqqi.roc().value()).isEqualByComparingTo("55.0");
        assertThat(qqqi.nasdaqTrend()).isEqualByComparingTo("-3.5");

        // 업데이트된 데이터로 리스크 재분석
        RiskMetrics updatedRisk = qqqi.analyzeRisk();
        assertThat(updatedRisk.overallRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    @Test
    @DisplayName("QQQI의 커버드콜 전략 특성 - ROC 100%도 정상일 수 있음")
    void understandingCoveredCallStrategy() {
        // Given: QQQI의 커버드콜 전략은 옵션 프리미엄으로 분배금 지급
        // ROC 100%는 자본 반환이지만, 옵션 수익으로 보충됨
        QQQI qqqi = createQQQI(
            ROC.of("100.0"),
            new BigDecimal("10.0")  // 나스닥 강세
        );

        // When
        RiskMetrics riskMetrics = qqqi.analyzeRisk();

        // Then: 경고는 나오지만, 나스닥 상승 시 옵션 수익으로 충당 가능
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.HIGH);

        // 나스닥 상승은 긍정적 신호
        RiskMetrics.RiskFactor nasdaqFactor = riskMetrics.riskFactors().stream()
            .filter(f -> f.category().contains("나스닥"))
            .findFirst()
            .orElseThrow();

        assertThat(nasdaqFactor.level()).isEqualTo(RiskLevel.LOW);
    }

    private QQQI createQQQI(ROC roc, BigDecimal nasdaqTrend) {
        ETFSnapshot snapshot = new ETFSnapshot(
            "QQQI",
            Money.of("55.50"),
            Money.of("53.05"),
            LocalDate.now()
        );
        return QQQI.create(snapshot, roc, nasdaqTrend, Money.of("0.6445"));
    }
}
