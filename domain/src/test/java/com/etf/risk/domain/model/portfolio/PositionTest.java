package com.etf.risk.domain.model.portfolio;

import com.etf.risk.domain.exception.InsufficientQuantityException;
import com.etf.risk.domain.exception.InvalidQuantityException;
import com.etf.risk.domain.model.common.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;

@DisplayName("Position 도메인 모델 테스트")
class PositionTest {

    @Test
    @DisplayName("초기 포지션 생성 시 평단가가 매수가와 동일하다")
    void createPosition() {
        // Given
        String symbol = "GOF";
        int quantity = 10;
        Money averagePrice = Money.of("20.00");

        // When
        Position position = Position.create(symbol, quantity, averagePrice);

        // Then
        assertThat(position.getSymbol()).isEqualTo(symbol);
        assertThat(position.getQuantity()).isEqualTo(quantity);
        assertThat(position.getAveragePrice()).isEqualTo(averagePrice);
    }

    @Test
    @DisplayName("추가 매수 시 평단가가 가중평균으로 재계산된다")
    void addQuantity_recalculatesAveragePrice() {
        // Given: GOF 10주를 $20에 매수
        Position position = Position.create("GOF", 10, Money.of("20.00"));

        // When: 추가로 10주를 $24에 매수
        position.addQuantity(10, Money.of("24.00"));

        // Then: 총 20주, 평단가 $22 (10*20 + 10*24) / 20 = 22
        assertThat(position.getQuantity()).isEqualTo(20);
        assertThat(position.getAveragePrice()).isEqualTo(Money.of("22.00"));
    }

    @Test
    @DisplayName("추가 매수 시 다양한 시나리오의 평단가 계산이 정확하다")
    void addQuantity_variousScenarios() {
        // Scenario 1: GOF 5주를 $21.50에 매수 후 15주를 $19.80에 추가 매수
        Position position1 = Position.create("GOF", 5, Money.of("21.50"));
        position1.addQuantity(15, Money.of("19.80"));

        // (5 * 21.50 + 15 * 19.80) / 20 = 20.225
        assertThat(position1.getQuantity()).isEqualTo(20);
        assertThat(position1.getAveragePrice()).isEqualTo(Money.of("20.225"));

        // Scenario 2: QQQI 100주를 $55.00에 매수 후 50주를 $56.50에 추가 매수
        Position position2 = Position.create("QQQI", 100, Money.of("55.00"));
        position2.addQuantity(50, Money.of("56.50"));

        // (100 * 55.00 + 50 * 56.50) / 150 = 55.50
        assertThat(position2.getQuantity()).isEqualTo(150);
        assertThat(position2.getAveragePrice()).isEqualTo(Money.of("55.50"));
    }

    @Test
    @DisplayName("일부 매도 시 수량만 감소하고 평단가는 유지된다")
    void reduceQuantity_maintainsAveragePrice() {
        // Given: GOF 20주를 평단가 $22에 보유
        Position position = Position.create("GOF", 10, Money.of("20.00"));
        position.addQuantity(10, Money.of("24.00"));

        Money originalAveragePrice = position.getAveragePrice();

        // When: 5주 매도
        position.reduceQuantity(5);

        // Then: 수량은 15주로 감소, 평단가는 $22 유지
        assertThat(position.getQuantity()).isEqualTo(15);
        assertThat(position.getAveragePrice()).isEqualTo(originalAveragePrice);
    }

    @Test
    @DisplayName("전량 매도 시 수량이 0이 된다")
    void reduceQuantity_toZero() {
        // Given
        Position position = Position.create("GOF", 10, Money.of("20.00"));

        // When
        position.reduceQuantity(10);

        // Then
        assertThat(position.getQuantity()).isEqualTo(0);
    }

    @Test
    @DisplayName("보유 수량보다 많이 매도하려고 하면 예외가 발생한다")
    void reduceQuantity_exceedsHolding_throwsException() {
        // Given
        Position position = Position.create("GOF", 10, Money.of("20.00"));

        // When & Then
        assertThatThrownBy(() -> position.reduceQuantity(11))
            .isInstanceOf(InsufficientQuantityException.class)
            .hasMessageContaining("보유 수량보다 많이 매도할 수 없습니다");
    }

    @Test
    @DisplayName("0 이하의 수량으로 매도하려고 하면 예외가 발생한다")
    void reduceQuantity_invalidQuantity_throwsException() {
        // Given
        Position position = Position.create("GOF", 10, Money.of("20.00"));

        // When & Then
        assertThatThrownBy(() -> position.reduceQuantity(0))
            .isInstanceOf(InvalidQuantityException.class);

        assertThatThrownBy(() -> position.reduceQuantity(-5))
            .isInstanceOf(InvalidQuantityException.class);
    }

    @Test
    @DisplayName("현재 가치 계산이 정확하다 (수량 × 현재가)")
    void calculateValue() {
        // Given: GOF 15주를 평단가 $22에 보유
        Position position = Position.create("GOF", 10, Money.of("20.00"));
        position.addQuantity(5, Money.of("26.00"));

        Money currentPrice = Money.of("25.00");

        // When
        Money value = position.calculateValue(currentPrice);

        // Then: 15주 × $25 = $375
        assertThat(value).isEqualTo(Money.of("375.00"));
    }

    @Test
    @DisplayName("손익률 계산 - 수익 상황 (현재가 > 평단가)")
    void calculateProfitLossRate_profit() {
        // Given: GOF를 평단가 $20에 보유
        Position position = Position.create("GOF", 10, Money.of("20.00"));
        Money currentPrice = Money.of("25.00");

        // When: 현재가 $25
        BigDecimal profitLossRate = position.calculateProfitLossRate(currentPrice);

        // Then: (25 - 20) / 20 * 100 = 25%
        assertThat(profitLossRate).isEqualByComparingTo("25.00");
    }

    @Test
    @DisplayName("손익률 계산 - 손실 상황 (현재가 < 평단가)")
    void calculateProfitLossRate_loss() {
        // Given: GOF를 평단가 $22에 보유
        Position position = Position.create("GOF", 10, Money.of("20.00"));
        position.addQuantity(10, Money.of("24.00"));

        Money currentPrice = Money.of("18.00");

        // When
        BigDecimal profitLossRate = position.calculateProfitLossRate(currentPrice);

        // Then: (18 - 22) / 22 * 100 = -18.18%
        assertThat(profitLossRate).isEqualByComparingTo("-18.18");
    }

    @Test
    @DisplayName("손익률 계산 - 손절 기준 (-15%) 판단")
    void calculateProfitLossRate_stopLossScenario() {
        // Given: 평단가 $20에 매수
        Position position = Position.create("GOF", 100, Money.of("20.00"));

        // When: 현재가 $17 (-15%)
        Money currentPrice1 = Money.of("17.00");
        BigDecimal lossRate1 = position.calculateProfitLossRate(currentPrice1);

        // When: 현재가 $16 (-20%)
        Money currentPrice2 = Money.of("16.00");
        BigDecimal lossRate2 = position.calculateProfitLossRate(currentPrice2);

        // Then
        assertThat(lossRate1).isEqualByComparingTo("-15.00"); // 손절 경고 구간
        assertThat(lossRate2).isEqualByComparingTo("-20.00"); // 손절 권장 구간
    }

    @Test
    @DisplayName("예상 배당금 계산이 정확하다")
    void calculateExpectedDividend() {
        // Given: GOF 100주 보유
        Position position = Position.create("GOF", 100, Money.of("20.00"));
        Money dividendPerShare = Money.of("0.1821");

        // When
        Money expectedDividend = position.calculateExpectedDividend(dividendPerShare);

        // Then: 100 × $0.1821 = $18.21
        assertThat(expectedDividend).isEqualTo(Money.of("18.21"));
    }

    @Test
    @DisplayName("실제 GOF 투자 시나리오 - 분할 매수 후 일부 매도")
    void realWorldScenario_GOF() {
        // Given: GOF에 3번에 걸쳐 분할 매수
        Position position = Position.create("GOF", 50, Money.of("21.50")); // 1차: 50주 @ $21.50
        position.addQuantity(30, Money.of("19.80")); // 2차: 30주 @ $19.80
        position.addQuantity(20, Money.of("22.10")); // 3차: 20주 @ $22.10

        // 평단가 계산: (50*21.50 + 30*19.80 + 20*22.10) / 100 = 2111.00 / 100 = 21.11
        assertThat(position.getQuantity()).isEqualTo(100);
        assertThat(position.getAveragePrice()).isEqualTo(Money.of("21.11"));

        // When: 현재가 $23.50, 손익률 확인
        Money currentPrice = Money.of("23.50");
        BigDecimal profitRate = position.calculateProfitLossRate(currentPrice);

        // Then: (23.50 - 21.11) / 21.11 * 100 = 11.32%
        assertThat(profitRate.doubleValue()).isCloseTo(11.32, within(0.01));

        // When: 일부 매도 (30주)
        position.reduceQuantity(30);

        // Then: 70주 남음, 평단가 유지
        assertThat(position.getQuantity()).isEqualTo(70);
        assertThat(position.getAveragePrice()).isEqualTo(Money.of("21.11"));
    }

    @Test
    @DisplayName("실제 QQQI 투자 시나리오 - 고배당 ETF 월배당 계산")
    void realWorldScenario_QQQI() {
        // Given: QQQI 200주 보유, 평단가 $54.20
        Position position = Position.create("QQQI", 200, Money.of("54.20"));

        // When: 월 배당금 $0.6445 지급
        Money monthlyDividendPerShare = Money.of("0.6445");
        Money expectedMonthlyDividend = position.calculateExpectedDividend(monthlyDividendPerShare);

        // Then: 200 × $0.6445 = $128.90
        assertThat(expectedMonthlyDividend).isEqualTo(Money.of("128.90"));

        // When: 연 배당금 계산 (월 × 12)
        Money annualDividend = expectedMonthlyDividend.multiply(12);

        // Then: $128.90 × 12 = $1,546.80
        assertThat(annualDividend).isEqualTo(Money.of("1546.80"));

        // Then: 연 배당 수익률 = 1546.80 / (200 * 54.20) * 100 = 14.26%
        Money totalInvestment = position.getAveragePrice().multiply(position.getQuantity());
        BigDecimal yieldRate = annualDividend.getAmount()
            .divide(totalInvestment.getAmount(), 4, java.math.RoundingMode.HALF_UP)
            .multiply(BigDecimal.valueOf(100));

        assertThat(yieldRate.doubleValue()).isCloseTo(14.26, within(0.01));
    }
}
