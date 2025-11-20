package com.etf.risk.domain.model.etf;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.risk.RiskLevel;
import com.etf.risk.domain.model.risk.RiskMetrics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.*;

@DisplayName("GOF 리스크 분석 테스트")
class GOFTest {

    @Test
    @DisplayName("GOF 생성 - 안정적인 상태")
    void createGOF_stableState() {
        // Given
        ETFSnapshot snapshot = new ETFSnapshot(
            "GOF",
            Money.of("21.50"),
            Money.of("20.00"),
            LocalDate.now()
        );
        Premium premium = Premium.of("7.5");  // 7.5% 프리미엄 (안정)
        Leverage leverage = Leverage.of("25.0", "25.0");  // 레버리지 안정
        ROC roc = ROC.of("25.0");  // ROC 25% (정상)

        // When
        GOF gof = GOF.create(snapshot, premium, leverage, roc, Money.of("0.1821"));

        // Then
        assertThat(gof.symbol()).isEqualTo("GOF");
        assertThat(gof.currentPrice()).isEqualTo(Money.of("21.50"));
        assertThat(gof.premium()).isEqualTo(premium);
        assertThat(gof.leverage()).isEqualTo(leverage);
        assertThat(gof.roc()).isEqualTo(roc);
    }

    @Test
    @DisplayName("리스크 분석 - 모든 지표가 안정적인 경우 (LOW)")
    void analyzeRisk_allStable_returnsLow() {
        // Given: 프리미엄 8%, 레버리지 안정, ROC 28%
        GOF gof = createGOF(
            Premium.of("8.0"),
            Leverage.of("25.0", "25.0"),
            ROC.of("28.0")
        );

        // When
        RiskMetrics riskMetrics = gof.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(riskMetrics.isStable()).isTrue();
        assertThat(riskMetrics.requiresAction()).isFalse();
    }

    @Test
    @DisplayName("리스크 분석 - 프리미엄 주의 구간 (10~15%)")
    void analyzeRisk_premiumWarning_returnsMedium() {
        // Given: 프리미엄 12%
        GOF gof = createGOF(
            Premium.of("12.0"),
            Leverage.of("25.0", "25.0"),
            ROC.of("25.0")
        );

        // When
        RiskMetrics riskMetrics = gof.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.MEDIUM);

        long premiumWarnings = riskMetrics.riskFactors().stream()
            .filter(f -> f.category().contains("프리미엄"))
            .filter(f -> f.level() == RiskLevel.MEDIUM)
            .count();
        assertThat(premiumWarnings).isEqualTo(1);
    }

    @Test
    @DisplayName("리스크 분석 - 프리미엄 위험 구간 (>15%, 신규 매수 금지)")
    void analyzeRisk_premiumDanger_returnsHigh() {
        // Given: 프리미엄 18%
        GOF gof = createGOF(
            Premium.of("18.0"),
            Leverage.of("25.0", "25.0"),
            ROC.of("25.0")
        );

        // When
        RiskMetrics riskMetrics = gof.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.HIGH);
        assertThat(riskMetrics.requiresAction()).isTrue();

        RiskMetrics.RiskFactor premiumFactor = riskMetrics.riskFactors().stream()
            .filter(f -> f.category().contains("프리미엄"))
            .findFirst()
            .orElseThrow();

        assertThat(premiumFactor.level()).isEqualTo(RiskLevel.HIGH);
        assertThat(premiumFactor.message()).contains("신규 매수 금지");
    }

    @Test
    @DisplayName("리스크 분석 - 레버리지 증가 시 리스크 상승")
    void analyzeRisk_leverageIncreasing_returnsMedium() {
        // Given: 레버리지 25% → 28% 증가
        GOF gof = createGOF(
            Premium.of("8.0"),
            Leverage.of("28.0", "25.0"),  // 증가
            ROC.of("25.0")
        );

        // When
        RiskMetrics riskMetrics = gof.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.MEDIUM);

        RiskMetrics.RiskFactor leverageFactor = riskMetrics.riskFactors().stream()
            .filter(f -> f.category().contains("레버리지"))
            .findFirst()
            .orElseThrow();

        assertThat(leverageFactor.level()).isEqualTo(RiskLevel.MEDIUM);
        assertThat(leverageFactor.message()).contains("증가");
    }

    @Test
    @DisplayName("리스크 분석 - ROC 주의 구간 (30~50%)")
    void analyzeRisk_rocWarning_returnsMedium() {
        // Given: ROC 40%
        GOF gof = createGOF(
            Premium.of("8.0"),
            Leverage.of("25.0", "25.0"),
            ROC.of("40.0")
        );

        // When
        RiskMetrics riskMetrics = gof.analyzeRisk();

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
    @DisplayName("리스크 분석 - ROC 위험 구간 (>50%, NAV 잠식)")
    void analyzeRisk_rocCritical_returnsCritical() {
        // Given: ROC 54.84% (실제 2024년 데이터)
        GOF gof = createGOF(
            Premium.of("8.0"),
            Leverage.of("25.0", "25.0"),
            ROC.of("54.84")
        );

        // When
        RiskMetrics riskMetrics = gof.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(riskMetrics.requiresAction()).isTrue();

        RiskMetrics.RiskFactor rocFactor = riskMetrics.riskFactors().stream()
            .filter(f -> f.category().contains("ROC"))
            .findFirst()
            .orElseThrow();

        assertThat(rocFactor.level()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(rocFactor.message()).contains("NAV 잠식");
    }

    @Test
    @DisplayName("리스크 분석 - 복합 위험 상황 (프리미엄 높음 + ROC 높음)")
    void analyzeRisk_multipleHighRisks_returnsCritical() {
        // Given: 프리미엄 16%, 레버리지 증가, ROC 55%
        GOF gof = createGOF(
            Premium.of("16.0"),    // HIGH
            Leverage.of("28.0", "25.0"),  // MEDIUM
            ROC.of("55.0")         // CRITICAL
        );

        // When
        RiskMetrics riskMetrics = gof.analyzeRisk();

        // Then: 최고 레벨인 CRITICAL 선택
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.CRITICAL);
        assertThat(riskMetrics.requiresAction()).isTrue();

        // 3개의 위험 요소 모두 감지
        long highRiskFactors = riskMetrics.riskFactors().stream()
            .filter(f -> f.level().ordinal() >= RiskLevel.MEDIUM.ordinal())
            .count();
        assertThat(highRiskFactors).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("실제 시나리오 - 2024년 GOF 상태 (ROC 54.84%)")
    void realWorldScenario_2024GOF() {
        // Given: 2024년 실제 GOF 데이터
        ETFSnapshot snapshot = new ETFSnapshot(
            "GOF",
            Money.of("21.82"),  // 현재가
            Money.of("20.15"),  // NAV
            LocalDate.of(2024, 11, 20)
        );

        GOF gof = GOF.create(
            snapshot,
            Premium.of("8.29"),    // 8.29% 프리미엄 (안정)
            Leverage.of("26.5", "26.0"),  // 레버리지 약간 증가
            ROC.of("54.84"),       // ROC 54.84% (위험)
            Money.of("0.1821")     // 월 배당 $0.1821
        );

        // When
        RiskMetrics riskMetrics = gof.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.CRITICAL);

        // 프리미엄: LOW (8.29% < 10%)
        // 레버리지: MEDIUM (증가)
        // ROC: CRITICAL (54.84% > 50%)
        assertThat(riskMetrics.riskFactors()).hasSize(4); // 3개 + 배당 지속성
    }

    @Test
    @DisplayName("실제 시나리오 - 이상적인 GOF 투자 시점")
    void realWorldScenario_idealEntryPoint() {
        // Given: 이상적인 진입 시점
        // - 프리미엄 낮음 (5% 이하)
        // - 레버리지 안정
        // - ROC 낮음 (25% 이하)
        GOF gof = createGOF(
            Premium.of("4.5"),
            Leverage.of("24.0", "24.0"),
            ROC.of("23.0")
        );

        // When
        RiskMetrics riskMetrics = gof.analyzeRisk();

        // Then
        assertThat(riskMetrics.overallRiskLevel()).isEqualTo(RiskLevel.LOW);
        assertThat(riskMetrics.isStable()).isTrue();

        // 모든 요소가 LOW 레벨
        long lowRiskFactors = riskMetrics.riskFactors().stream()
            .filter(f -> f.level() == RiskLevel.LOW)
            .count();
        assertThat(lowRiskFactors).isEqualTo(4);
    }

    @Test
    @DisplayName("연 배당 수익률 계산")
    void calculateYield() {
        // Given: GOF 현재가 $21.82
        ETFSnapshot snapshot = new ETFSnapshot(
            "GOF",
            Money.of("21.82"),
            Money.of("20.15"),
            LocalDate.now()
        );
        GOF gof = GOF.create(snapshot, null, null, null, null);

        // When: 월 배당 $0.1821 × 12 = $2.1852
        Money annualDividend = Money.of("0.1821").multiply(12);
        BigDecimal yieldRate = gof.calculateYield(annualDividend);

        // Then: 2.1852 / 21.82 * 100 = 10.01%
        assertThat(yieldRate.doubleValue()).isCloseTo(10.01, within(0.01));
    }

    @Test
    @DisplayName("ETFSnapshot 업데이트")
    void updateSnapshot() {
        // Given
        GOF gof = createGOF(Premium.of("8.0"), null, null);
        Money originalPrice = gof.currentPrice();

        // When: 새로운 스냅샷으로 업데이트
        ETFSnapshot newSnapshot = new ETFSnapshot(
            "GOF",
            Money.of("23.50"),
            Money.of("21.00"),
            LocalDate.now()
        );
        gof.updateSnapshot(newSnapshot);

        // Then
        assertThat(gof.currentPrice()).isEqualTo(Money.of("23.50"));
        assertThat(gof.currentPrice()).isNotEqualTo(originalPrice);
    }

    @Test
    @DisplayName("Premium/ROC/Leverage 업데이트")
    void updateRiskFactors() {
        // Given
        GOF gof = createGOF(
            Premium.of("8.0"),
            Leverage.of("25.0", null),
            ROC.of("25.0")
        );

        // When
        gof.updatePremium(Premium.of("12.0"));
        gof.updateLeverage(Leverage.of("28.0", "25.0"));
        gof.updateROC(ROC.of("45.0"));

        // Then
        assertThat(gof.premium().value()).isEqualByComparingTo("12.0");
        assertThat(gof.leverage().current()).isEqualByComparingTo("28.0");
        assertThat(gof.roc().value()).isEqualByComparingTo("45.0");

        // 업데이트된 데이터로 리스크 재분석
        RiskMetrics updatedRisk = gof.analyzeRisk();
        assertThat(updatedRisk.overallRiskLevel()).isEqualTo(RiskLevel.MEDIUM);
    }

    private GOF createGOF(Premium premium, Leverage leverage, ROC roc) {
        ETFSnapshot snapshot = new ETFSnapshot(
            "GOF",
            Money.of("21.50"),
            Money.of("20.00"),
            LocalDate.now()
        );
        return GOF.create(snapshot, premium, leverage, roc, Money.of("0.1821"));
    }
}
