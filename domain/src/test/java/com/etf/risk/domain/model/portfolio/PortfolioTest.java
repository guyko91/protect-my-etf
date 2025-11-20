package com.etf.risk.domain.model.portfolio;

import com.etf.risk.domain.exception.DuplicatePositionException;
import com.etf.risk.domain.exception.PositionNotFoundException;
import com.etf.risk.domain.model.common.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Portfolio 도메인 모델 테스트")
class PortfolioTest {

    @Test
    @DisplayName("빈 포트폴리오 생성")
    void createEmptyPortfolio() {
        // Given & When
        Portfolio portfolio = Portfolio.createEmpty();

        // Then
        assertThat(portfolio.isEmpty()).isTrue();
        assertThat(portfolio.getPositionCount()).isEqualTo(0);
        assertThat(portfolio.getPositions()).isEmpty();
    }

    @Test
    @DisplayName("포지션 추가 성공")
    void addPosition() {
        // Given
        Portfolio portfolio = Portfolio.createEmpty();

        // When
        portfolio.addPosition("GOF", 10, Money.of("20.00"));

        // Then
        assertThat(portfolio.isEmpty()).isFalse();
        assertThat(portfolio.getPositionCount()).isEqualTo(1);
        assertThat(portfolio.hasPosition("GOF")).isTrue();

        Position position = portfolio.getPosition("GOF");
        assertThat(position.getQuantity()).isEqualTo(10);
        assertThat(position.getAveragePrice()).isEqualTo(Money.of("20.00"));
    }

    @Test
    @DisplayName("중복 포지션 추가 시 예외 발생")
    void addPosition_duplicate_throwsException() {
        // Given
        Portfolio portfolio = Portfolio.createEmpty();
        portfolio.addPosition("GOF", 10, Money.of("20.00"));

        // When & Then
        assertThatThrownBy(() -> portfolio.addPosition("GOF", 5, Money.of("22.00")))
            .isInstanceOf(DuplicatePositionException.class)
            .hasMessageContaining("이미 보유 중인 ETF");
    }

    @Test
    @DisplayName("기존 포지션에 추가 매수")
    void addToPosition() {
        // Given: GOF 10주 보유
        Portfolio portfolio = Portfolio.createEmpty();
        portfolio.addPosition("GOF", 10, Money.of("20.00"));

        // When: 5주 추가 매수
        portfolio.addToPosition("GOF", 5, Money.of("24.00"));

        // Then: 15주, 평단가 재계산 (10*20 + 5*24) / 15 = 21.3333
        Position position = portfolio.getPosition("GOF");
        assertThat(position.getQuantity()).isEqualTo(15);
        assertThat(position.getAveragePrice()).isEqualTo(Money.of("21.3333"));
    }

    @Test
    @DisplayName("존재하지 않는 포지션에 추가 매수 시도 시 예외 발생")
    void addToPosition_notFound_throwsException() {
        // Given
        Portfolio portfolio = Portfolio.createEmpty();

        // When & Then
        assertThatThrownBy(() -> portfolio.addToPosition("GOF", 5, Money.of("20.00")))
            .isInstanceOf(PositionNotFoundException.class)
            .hasMessageContaining("ETF를 보유하고 있지 않습니다");
    }

    @Test
    @DisplayName("일부 매도 - 수량만 감소")
    void removeFromPosition_partial() {
        // Given: GOF 20주 보유
        Portfolio portfolio = Portfolio.createEmpty();
        portfolio.addPosition("GOF", 20, Money.of("21.00"));

        // When: 5주 매도
        portfolio.removeFromPosition("GOF", 5);

        // Then: 15주 남음
        assertThat(portfolio.hasPosition("GOF")).isTrue();
        assertThat(portfolio.getPosition("GOF").getQuantity()).isEqualTo(15);
    }

    @Test
    @DisplayName("전량 매도 - 포지션 자동 제거")
    void removeFromPosition_full() {
        // Given: GOF 10주 보유
        Portfolio portfolio = Portfolio.createEmpty();
        portfolio.addPosition("GOF", 10, Money.of("20.00"));

        // When: 10주 전량 매도
        portfolio.removeFromPosition("GOF", 10);

        // Then: 포지션 제거됨
        assertThat(portfolio.hasPosition("GOF")).isFalse();
        assertThat(portfolio.isEmpty()).isTrue();
    }

    @Test
    @DisplayName("포지션 명시적 제거")
    void removePosition() {
        // Given
        Portfolio portfolio = Portfolio.createEmpty();
        portfolio.addPosition("GOF", 10, Money.of("20.00"));
        portfolio.addPosition("QQQI", 20, Money.of("55.00"));

        // When
        portfolio.removePosition("GOF");

        // Then
        assertThat(portfolio.hasPosition("GOF")).isFalse();
        assertThat(portfolio.hasPosition("QQQI")).isTrue();
        assertThat(portfolio.getPositionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("존재하지 않는 포지션 제거 시도 시 예외 발생")
    void removePosition_notFound_throwsException() {
        // Given
        Portfolio portfolio = Portfolio.createEmpty();

        // When & Then
        assertThatThrownBy(() -> portfolio.removePosition("GOF"))
            .isInstanceOf(PositionNotFoundException.class);
    }

    @Test
    @DisplayName("포트폴리오 총 가치 계산")
    void calculateTotalValue() {
        // Given: GOF 10주 + QQQI 20주 보유
        Portfolio portfolio = Portfolio.createEmpty();
        portfolio.addPosition("GOF", 10, Money.of("20.00"));
        portfolio.addPosition("QQQI", 20, Money.of("50.00"));

        Map<String, Money> currentPrices = Map.of(
            "GOF", Money.of("25.00"),   // 10주 × $25 = $250
            "QQQI", Money.of("55.00")   // 20주 × $55 = $1,100
        );

        // When
        Money totalValue = portfolio.calculateTotalValue(currentPrices);

        // Then: $250 + $1,100 = $1,350
        assertThat(totalValue).isEqualTo(Money.of("1350.00"));
    }

    @Test
    @DisplayName("포지션 비중 계산 - 단일 종목")
    void calculateWeight_singlePosition() {
        // Given: GOF만 보유
        Portfolio portfolio = Portfolio.createEmpty();
        portfolio.addPosition("GOF", 10, Money.of("20.00"));

        Map<String, Money> currentPrices = Map.of("GOF", Money.of("25.00"));

        // When
        BigDecimal weight = portfolio.calculateWeight("GOF", currentPrices);

        // Then: 100%
        assertThat(weight).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("포지션 비중 계산 - 복수 종목")
    void calculateWeight_multiplePositions() {
        // Given: GOF 10주 ($25) + QQQI 20주 ($55) 보유
        Portfolio portfolio = Portfolio.createEmpty();
        portfolio.addPosition("GOF", 10, Money.of("20.00"));
        portfolio.addPosition("QQQI", 20, Money.of("50.00"));

        Map<String, Money> currentPrices = Map.of(
            "GOF", Money.of("25.00"),   // $250
            "QQQI", Money.of("55.00")   // $1,100
        );

        // When
        BigDecimal gofWeight = portfolio.calculateWeight("GOF", currentPrices);
        BigDecimal qqqiWeight = portfolio.calculateWeight("QQQI", currentPrices);

        // Then
        // GOF: 250 / 1350 * 100 = 18.52%
        // QQQI: 1100 / 1350 * 100 = 81.48%
        assertThat(gofWeight.doubleValue()).isCloseTo(18.52, within(0.01));
        assertThat(qqqiWeight.doubleValue()).isCloseTo(81.48, within(0.01));

        // 합계 검증
        BigDecimal totalWeight = gofWeight.add(qqqiWeight);
        assertThat(totalWeight.doubleValue()).isCloseTo(100.00, within(0.01));
    }

    @Test
    @DisplayName("실제 포트폴리오 시나리오 - GOF/QQQI 50:50 리밸런싱 목표")
    void realWorldScenario_balancedPortfolio() {
        // Given: 초기 투자 $10,000 목표, GOF/QQQI 50:50 비중
        Portfolio portfolio = Portfolio.createEmpty();

        // GOF: $5,000 투자, 현재가 $21.50 → 약 232주
        portfolio.addPosition("GOF", 232, Money.of("21.55"));

        // QQQI: $5,000 투자, 현재가 $54.00 → 약 92주
        portfolio.addPosition("QQQI", 92, Money.of("54.35"));

        // When: 1개월 후 가격 변동
        Map<String, Money> currentPrices = Map.of(
            "GOF", Money.of("23.10"),   // +7.2% 상승
            "QQQI", Money.of("52.80")   // -2.9% 하락
        );

        // Then: 포트폴리오 분석
        Money totalValue = portfolio.calculateTotalValue(currentPrices);
        BigDecimal gofWeight = portfolio.calculateWeight("GOF", currentPrices);
        BigDecimal qqqiWeight = portfolio.calculateWeight("QQQI", currentPrices);

        // 총 가치: 232*23.10 + 92*52.80 = 5,359.20 + 4,857.60 = 10,216.80
        assertThat(totalValue.getAmount().doubleValue()).isCloseTo(10216.80, within(1.0));

        // 비중: GOF 52.47%, QQQI 47.53% (약간 불균형)
        assertThat(gofWeight.doubleValue()).isCloseTo(52.47, within(0.5));
        assertThat(qqqiWeight.doubleValue()).isCloseTo(47.53, within(0.5));

        // 수익률 확인
        Position gofPosition = portfolio.getPosition("GOF");
        Position qqqiPosition = portfolio.getPosition("QQQI");

        BigDecimal gofReturn = gofPosition.calculateProfitLossRate(Money.of("23.10"));
        BigDecimal qqqiReturn = qqqiPosition.calculateProfitLossRate(Money.of("52.80"));

        assertThat(gofReturn.doubleValue()).isCloseTo(7.19, within(0.1)); // +7.19%
        assertThat(qqqiReturn.doubleValue()).isCloseTo(-2.85, within(0.1)); // -2.85%
    }

    @Test
    @DisplayName("실제 포트폴리오 시나리오 - 손절 기준 도달 시나리오")
    void realWorldScenario_stopLossCheck() {
        // Given: GOF 100주를 $22에 매수 (총 $2,200 투자)
        Portfolio portfolio = Portfolio.createEmpty();
        portfolio.addPosition("GOF", 100, Money.of("22.00"));

        // Scenario 1: 현재가 $18.70 (-15%, 손절 경고)
        Map<String, Money> scenario1 = Map.of("GOF", Money.of("18.70"));
        BigDecimal weight1 = portfolio.calculateWeight("GOF", scenario1);
        BigDecimal loss1 = portfolio.getPosition("GOF")
            .calculateProfitLossRate(Money.of("18.70"));

        assertThat(loss1.doubleValue()).isCloseTo(-15.0, within(0.1)); // -15% 손실
        assertThat(weight1).isEqualByComparingTo("100.00"); // 단일 종목 100%

        // Scenario 2: 현재가 $17.60 (-20%, 손절 권장)
        BigDecimal loss2 = portfolio.getPosition("GOF")
            .calculateProfitLossRate(Money.of("17.60"));

        assertThat(loss2.doubleValue()).isCloseTo(-20.0, within(0.1)); // -20% 손실 (손절 기준)
    }

    @Test
    @DisplayName("실제 포트폴리오 시나리오 - 복잡한 거래 이력")
    void realWorldScenario_complexTradingHistory() {
        // Given: 포트폴리오 구축 과정
        Portfolio portfolio = Portfolio.createEmpty();

        // 1단계: GOF 초기 매수
        portfolio.addPosition("GOF", 50, Money.of("21.00"));

        // 2단계: QQQI 초기 매수
        portfolio.addPosition("QQQI", 30, Money.of("54.00"));

        // 3단계: GOF 추가 매수 (하락 시 물타기)
        portfolio.addToPosition("GOF", 30, Money.of("19.50"));

        // 4단계: QQQI 추가 매수
        portfolio.addToPosition("QQQI", 20, Money.of("55.50"));

        // 5단계: GOF 일부 매도 (리밸런싱)
        portfolio.removeFromPosition("GOF", 20);

        // Then: 최종 포지션 확인
        Position gofPosition = portfolio.getPosition("GOF");
        Position qqqiPosition = portfolio.getPosition("QQQI");

        // GOF: (50*21 + 30*19.5) / 80 = 1635 / 80 = 20.4375, 매도 후 60주
        assertThat(gofPosition.getQuantity()).isEqualTo(60);
        assertThat(gofPosition.getAveragePrice()).isEqualTo(Money.of("20.4375"));

        // QQQI: (30*54 + 20*55.5) / 50 = 2730 / 50 = 54.6, 50주
        assertThat(qqqiPosition.getQuantity()).isEqualTo(50);
        assertThat(qqqiPosition.getAveragePrice()).isEqualTo(Money.of("54.6"));

        // 포트폴리오 분석
        Map<String, Money> currentPrices = Map.of(
            "GOF", Money.of("22.50"),
            "QQQI", Money.of("56.00")
        );

        Money totalValue = portfolio.calculateTotalValue(currentPrices);
        // 60*22.50 + 50*56.00 = 1,350 + 2,800 = 4,150
        assertThat(totalValue).isEqualTo(Money.of("4150.00"));

        BigDecimal gofWeight = portfolio.calculateWeight("GOF", currentPrices);
        BigDecimal qqqiWeight = portfolio.calculateWeight("QQQI", currentPrices);

        // GOF: 1350/4150*100 = 32.53%
        // QQQI: 2800/4150*100 = 67.47%
        assertThat(gofWeight.doubleValue()).isCloseTo(32.53, within(0.01));
        assertThat(qqqiWeight.doubleValue()).isCloseTo(67.47, within(0.01));
    }
}
