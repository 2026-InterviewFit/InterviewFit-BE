package com.example.interviewfitbe.auth;

import com.example.interviewfitbe.domain.group.controller.GroupInterviewController;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.service.GroupInterviewFlowService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewService;
import com.example.interviewfitbe.domain.livekit.dto.LiveKitTokenResponse;
import com.example.interviewfitbe.domain.livekit.service.LiveKitService;
import com.example.interviewfitbe.domain.user.client.KakaoApiClient;
import com.example.interviewfitbe.domain.user.controller.UserController;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.service.UserService;
import com.example.interviewfitbe.global.config.SecurityConfig;
import com.example.interviewfitbe.global.jwt.JwtCookieProvider;
import com.example.interviewfitbe.global.jwt.JwtTokenProvider;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest({GroupInterviewController.class, UserController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class, JwtCookieProvider.class})
@TestPropertySource(properties = {
        "jwt.secret=local-test-secret-that-is-long-enough-for-hs256-signing",
        "jwt.access-expiration-ms=1800000",
        "jwt.refresh-expiration-ms=1209600000",
        "jwt.cookie.secure=false",
        "jwt.cookie.same-site=Lax",
        "app.cors.allowed-origins=http://localhost:3000"
})
class CookieAuthenticationIntegrationTest {

    private static final Long USER_ID = 42L;

    @Autowired
    MockMvc mockMvc;

    @Autowired
    JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    GroupInterviewService groupInterviewService;

    @MockitoBean
    GroupInterviewFlowService groupInterviewFlowService;

    @MockitoBean
    LiveKitService liveKitService;

    @MockitoBean
    UserService userService;

    @MockitoBean
    KakaoApiClient kakaoApiClient;

    private Cookie accessCookie;

    @BeforeEach
    void setUp() {
        accessCookie = new Cookie(
                JwtCookieProvider.ACCESS_TOKEN_COOKIE_NAME,
                jwtTokenProvider.createAccessToken(USER_ID)
        );
    }

    @Test
    void groupGetApisAcceptAccessTokenCookie() throws Exception {
        when(groupInterviewService.getRoomById(1L))
                .thenReturn(GroupInterviewEntity.builder().title("room").build());
        when(groupInterviewService.getParticipants(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/group-interviews/1").cookie(accessCookie))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/group-interviews/1/participants").cookie(accessCookie))
                .andExpect(status().isOk());
    }

    @Test
    void createRoomUsesAuthenticatedUserId() throws Exception {
        when(groupInterviewService.createRoom(any(), eq(USER_ID), any(), any(), any()))
                .thenReturn(GroupInterviewEntity.builder().title("room").build());

        mockMvc.perform(post("/api/group-interviews")
                        .cookie(accessCookie)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"title":"room","jobRoleId":1,"maxParticipants":2}
                                """))
                .andExpect(status().isOk());

        verify(groupInterviewService)
                .createRoom(eq("room"), eq(USER_ID), eq(1L), eq(2), any());
    }

    @Test
    void joinRoomUsesAuthenticatedUserId() throws Exception {
        when(groupInterviewService.joinRoom(1L, USER_ID))
                .thenReturn(GroupParticipantEntity.builder()
                        .roomId(1L).userId(USER_ID).status("JOINED").build());

        mockMvc.perform(post("/api/group-interviews/1/join").cookie(accessCookie))
                .andExpect(status().isOk());

        verify(groupInterviewService).joinRoom(1L, USER_ID);
    }

    @Test
    void liveKitTokenUsesAuthenticatedUserId() throws Exception {
        when(liveKitService.createGroupInterviewToken(1L, USER_ID))
                .thenReturn(new LiveKitTokenResponse("wss://livekit", "token"));

        mockMvc.perform(post("/api/group-interviews/1/livekit-token").cookie(accessCookie))
                .andExpect(status().isOk());

        verify(liveKitService).createGroupInterviewToken(1L, USER_ID);
    }

    @Test
    void protectedGroupPostWithoutCookieReturns401() throws Exception {
        mockMvc.perform(post("/api/group-interviews/1/join"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshCookieIssuesNewAccessTokenCookie() throws Exception {
        String refreshToken = jwtTokenProvider.createRefreshToken(USER_ID);
        when(userService.getUserById(USER_ID)).thenReturn(mock(UserEntity.class));

        mockMvc.perform(post("/api/users/refresh")
                        .cookie(new Cookie(
                                JwtCookieProvider.REFRESH_TOKEN_COOKIE_NAME,
                                refreshToken
                        )))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString(
                                JwtCookieProvider.ACCESS_TOKEN_COOKIE_NAME + "="
                        )
                ))
                .andExpect(header().string(
                        HttpHeaders.SET_COOKIE,
                        org.hamcrest.Matchers.containsString("HttpOnly")
                ));
    }

    @Test
    void corsAllowsConfiguredFrontendWithCredentials() throws Exception {
        mockMvc.perform(options("/api/group-interviews/1")
                        .header(HttpHeaders.ORIGIN, "http://localhost:3000")
                        .header(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "GET"))
                .andExpect(status().isOk())
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN,
                        "http://localhost:3000"
                ))
                .andExpect(header().string(
                        HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS,
                        "true"
                ));
    }
}
