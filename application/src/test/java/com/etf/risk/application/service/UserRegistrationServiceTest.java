package com.etf.risk.application.service;

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

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserRegistrationService 통합 테스트")
class UserRegistrationServiceTest {

    @Mock
    private UserRepository userRepository;

    private UserRegistrationService userRegistrationService;

    @BeforeEach
    void setUp() {
        userRegistrationService = new UserRegistrationService(userRepository);
    }

    @Nested
    @DisplayName("registerUser 메서드")
    class RegisterUser {

        @Test
        @DisplayName("신규 사용자 등록 성공")
        void registerNewUser_Success() {
            // given
            TelegramChatId chatId = new TelegramChatId(123456789L);
            String username = "testuser";

            given(userRepository.existsByTelegramChatId(chatId)).willReturn(false);
            given(userRepository.save(any(User.class))).willAnswer(invocation -> {
                User user = invocation.getArgument(0);
                user.setId(1L);
                return user;
            });

            // when
            User result = userRegistrationService.registerUser(chatId, username);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
            assertThat(result.getTelegramChatId()).isEqualTo(chatId);
            assertThat(result.getTelegramUsername()).isEqualTo(username);
            verify(userRepository).save(any(User.class));
        }

        @Test
        @DisplayName("이미 등록된 사용자는 예외 발생")
        void registerExistingUser_ThrowsException() {
            // given
            TelegramChatId chatId = new TelegramChatId(123456789L);
            given(userRepository.existsByTelegramChatId(chatId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> userRegistrationService.registerUser(chatId, "testuser"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("이미 등록된 사용자");
        }
    }

    @Nested
    @DisplayName("findUserByChatId 메서드")
    class FindUserByChatId {

        @Test
        @DisplayName("존재하는 사용자 조회 성공")
        void findExistingUser_Success() {
            // given
            TelegramChatId chatId = new TelegramChatId(123456789L);
            User existingUser = User.register(chatId, "testuser");
            existingUser.setId(1L);

            given(userRepository.findByTelegramChatId(chatId)).willReturn(Optional.of(existingUser));

            // when
            User result = userRegistrationService.findUserByChatId(chatId);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getTelegramChatId()).isEqualTo(chatId);
        }

        @Test
        @DisplayName("존재하지 않는 사용자 조회시 예외 발생")
        void findNonExistingUser_ThrowsException() {
            // given
            TelegramChatId chatId = new TelegramChatId(999999999L);
            given(userRepository.findByTelegramChatId(chatId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> userRegistrationService.findUserByChatId(chatId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    @Nested
    @DisplayName("isUserRegistered 메서드")
    class IsUserRegistered {

        @Test
        @DisplayName("등록된 사용자는 true 반환")
        void registeredUser_ReturnsTrue() {
            // given
            TelegramChatId chatId = new TelegramChatId(123456789L);
            given(userRepository.existsByTelegramChatId(chatId)).willReturn(true);

            // when
            boolean result = userRegistrationService.isUserRegistered(chatId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("미등록 사용자는 false 반환")
        void unregisteredUser_ReturnsFalse() {
            // given
            TelegramChatId chatId = new TelegramChatId(999999999L);
            given(userRepository.existsByTelegramChatId(chatId)).willReturn(false);

            // when
            boolean result = userRegistrationService.isUserRegistered(chatId);

            // then
            assertThat(result).isFalse();
        }
    }
}
