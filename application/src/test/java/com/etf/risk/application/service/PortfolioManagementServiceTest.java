package com.etf.risk.application.service;

import com.etf.risk.domain.model.common.Money;
import com.etf.risk.domain.model.portfolio.Position;
import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.out.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("PortfolioManagementService 통합 테스트")
class PortfolioManagementServiceTest {

    @Mock
    private UserRepository userRepository;

    private PortfolioManagementService portfolioManagementService;

    private User testUser;

    @BeforeEach
    void setUp() {
        portfolioManagementService = new PortfolioManagementService(userRepository);
        testUser = User.register(new TelegramChatId(123456789L), "testuser");
        testUser.setId(1L);
    }

    @Nested
    @DisplayName("addPosition 메서드")
    class AddPosition {

        @Test
        @DisplayName("신규 포지션 추가 성공")
        void addNewPosition_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            portfolioManagementService.addPosition(1L, "GOF", 100, Money.of("20.50"));

            // then
            assertThat(testUser.hasPosition("GOF")).isTrue();
            Position position = testUser.getPosition("GOF");
            assertThat(position.getQuantity()).isEqualTo(100);
            assertThat(position.getAveragePrice()).isEqualTo(Money.of("20.50"));
            verify(userRepository).save(testUser);
        }

        @Test
        @DisplayName("존재하지 않는 사용자는 예외 발생")
        void addPositionToNonExistingUser_ThrowsException() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() ->
                    portfolioManagementService.addPosition(999L, "GOF", 100, Money.of("20.50")))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("addToPosition 메서드")
    class AddToPosition {

        @Test
        @DisplayName("기존 포지션 추가 매수 성공")
        void addToExistingPosition_Success() {
            // given
            testUser.addPosition("GOF", 100, Money.of("20.00"));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            portfolioManagementService.addToPosition(1L, "GOF", 50, Money.of("22.00"));

            // then
            Position position = testUser.getPosition("GOF");
            assertThat(position.getQuantity()).isEqualTo(150);
            verify(userRepository).save(testUser);
        }
    }

    @Nested
    @DisplayName("removePosition 메서드")
    class RemovePosition {

        @Test
        @DisplayName("포지션 제거 성공")
        void removePosition_Success() {
            // given
            testUser.addPosition("GOF", 100, Money.of("20.00"));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            portfolioManagementService.removePosition(1L, "GOF");

            // then
            assertThat(testUser.hasPosition("GOF")).isFalse();
            verify(userRepository).save(testUser);
        }
    }

    @Nested
    @DisplayName("getUserPositions 메서드")
    class GetUserPositions {

        @Test
        @DisplayName("사용자 포트폴리오 조회 성공")
        void getUserPositions_Success() {
            // given
            testUser.addPosition("GOF", 100, Money.of("20.00"));
            testUser.addPosition("QQQI", 50, Money.of("54.00"));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            List<Position> positions = portfolioManagementService.getUserPositions(1L);

            // then
            assertThat(positions).hasSize(2);
            assertThat(positions).extracting(Position::getSymbol)
                    .containsExactlyInAnyOrder("GOF", "QQQI");
        }

        @Test
        @DisplayName("빈 포트폴리오 조회")
        void getEmptyPortfolio_ReturnsEmptyList() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            List<Position> positions = portfolioManagementService.getUserPositions(1L);

            // then
            assertThat(positions).isEmpty();
        }
    }

    @Nested
    @DisplayName("getUserPosition 메서드")
    class GetUserPosition {

        @Test
        @DisplayName("특정 포지션 조회 성공")
        void getUserPosition_Success() {
            // given
            testUser.addPosition("GOF", 100, Money.of("20.00"));
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when
            Position position = portfolioManagementService.getUserPosition(1L, "GOF");

            // then
            assertThat(position).isNotNull();
            assertThat(position.getSymbol()).isEqualTo("GOF");
            assertThat(position.getQuantity()).isEqualTo(100);
        }

        @Test
        @DisplayName("존재하지 않는 포지션 조회시 예외 발생")
        void getNonExistingPosition_ThrowsException() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(testUser));

            // when & then
            assertThatThrownBy(() -> portfolioManagementService.getUserPosition(1L, "QQQI"))
                    .isInstanceOf(com.etf.risk.domain.exception.PositionNotFoundException.class);
        }
    }
}
