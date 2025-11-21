package com.etf.risk.adapter.web.controller;

import com.etf.risk.adapter.web.exception.GlobalExceptionHandler;
import com.etf.risk.domain.model.user.TelegramChatId;
import com.etf.risk.domain.model.user.User;
import com.etf.risk.domain.port.in.RegisterUserUseCase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserController 통합 테스트")
class UserControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock
    private RegisterUserUseCase registerUserUseCase;

    @BeforeEach
    void setUp() {
        UserController userController = new UserController(registerUserUseCase);
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        objectMapper = new ObjectMapper();
    }

    @Nested
    @DisplayName("POST /api/users/register")
    class RegisterUser {

        @Test
        @DisplayName("사용자 등록 성공")
        void registerUser_Success() throws Exception {
            // given
            TelegramChatId chatId = new TelegramChatId(123456789L);
            User user = User.register(chatId, "testuser");
            user.setId(1L);

            given(registerUserUseCase.registerUser(any(TelegramChatId.class), anyString()))
                    .willReturn(user);

            Map<String, Object> request = Map.of(
                    "telegramChatId", 123456789L,
                    "username", "testuser"
            );

            // when & then
            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.telegramChatId").value(123456789))
                    .andExpect(jsonPath("$.data.telegramUsername").value("testuser"));
        }

        @Test
        @DisplayName("유효성 검사 실패 - telegramChatId 누락")
        void registerUser_ValidationFailed() throws Exception {
            // given
            Map<String, Object> request = Map.of(
                    "username", "testuser"
            );

            // when & then
            mockMvc.perform(post("/api/users/register")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/users/chat/{chatId}/exists")
    class CheckUserExists {

        @Test
        @DisplayName("등록된 사용자 확인")
        void checkExistingUser_ReturnsTrue() throws Exception {
            // given
            given(registerUserUseCase.isUserRegistered(any(TelegramChatId.class)))
                    .willReturn(true);

            // when & then
            mockMvc.perform(get("/api/users/chat/123456789/exists"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(true));
        }

        @Test
        @DisplayName("미등록 사용자 확인")
        void checkNonExistingUser_ReturnsFalse() throws Exception {
            // given
            given(registerUserUseCase.isUserRegistered(any(TelegramChatId.class)))
                    .willReturn(false);

            // when & then
            mockMvc.perform(get("/api/users/chat/999999999/exists"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data").value(false));
        }
    }

    @Nested
    @DisplayName("GET /api/users/chat/{chatId}")
    class GetUserByChatId {

        @Test
        @DisplayName("사용자 조회 성공")
        void getUserByChatId_Success() throws Exception {
            // given
            TelegramChatId chatId = new TelegramChatId(123456789L);
            User user = User.register(chatId, "testuser");
            user.setId(1L);

            given(registerUserUseCase.findUserByChatId(any(TelegramChatId.class)))
                    .willReturn(user);

            // when & then
            mockMvc.perform(get("/api/users/chat/123456789"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.telegramChatId").value(123456789));
        }
    }
}
