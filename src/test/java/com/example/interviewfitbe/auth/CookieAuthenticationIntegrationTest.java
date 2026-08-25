package com.example.interviewfitbe.auth;

import com.example.interviewfitbe.domain.group.controller.GroupInterviewController;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.dto.GroupParticipantResponse;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewAnswerResponse;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewComparisonResponse;
import com.example.interviewfitbe.domain.group.entity.GroupComparisonScope;
import com.example.interviewfitbe.domain.group.entity.GroupComparisonStatus;
import com.example.interviewfitbe.domain.group.service.GroupInterviewFlowService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewAnswerService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewAnswerAnalysisService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewDashboardService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewComparisonService;
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
import org.springframework.mock.web.MockMultipartFile;

import java.util.List;
import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

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
    GroupInterviewAnswerService groupInterviewAnswerService;

    @MockitoBean
    GroupInterviewAnswerAnalysisService groupInterviewAnswerAnalysisService;

    @MockitoBean
    GroupInterviewDashboardService groupInterviewDashboardService;

    @MockitoBean
    GroupInterviewComparisonService groupInterviewComparisonService;

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
                .thenReturn(GroupParticipantResponse.builder()
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
    void participantListWithoutCookieReturns401() throws Exception {
        mockMvc.perform(get("/api/group-interviews/1/participants"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void readyAndLeaveUseAuthenticatedUserId() throws Exception {
        when(groupInterviewService.setReady(1L, USER_ID, true))
                .thenReturn(GroupParticipantResponse.builder()
                        .roomId(1L).userId(USER_ID).status("READY").build());

        mockMvc.perform(post("/api/group-interviews/1/ready").cookie(accessCookie))
                .andExpect(status().isOk());
        mockMvc.perform(delete("/api/group-interviews/1/participants/me").cookie(accessCookie))
                .andExpect(status().isNoContent());

        verify(groupInterviewService).setReady(1L, USER_ID, true);
        verify(groupInterviewService).leaveRoom(1L, USER_ID);
    }

    @Test
    void interviewControlUsesAuthenticatedHostIdAndNoParticipantCount() throws Exception {
        mockMvc.perform(post("/api/group-interviews/1/start").cookie(accessCookie))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/group-interviews/1/answer/end").cookie(accessCookie))
                .andExpect(status().isOk());

        verify(groupInterviewFlowService).start(1L, USER_ID);
        verify(groupInterviewFlowService).completeAnswer(1L, USER_ID);
    }

    @Test
    void groupAnswerVideoUploadUsesAuthenticatedUserId() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "answer.webm", "video/webm", new byte[]{1, 2, 3}
        );
        when(groupInterviewAnswerService.uploadVideo(1L, 123L, USER_ID, file))
                .thenReturn(GroupInterviewAnswerResponse.builder()
                        .answerId(123L)
                        .roomId(1L)
                        .participantUserId(USER_ID)
                        .videoUploaded(true)
                        .build());

        mockMvc.perform(multipart("/api/group-interviews/1/answers/123/video")
                        .file(file)
                        .cookie(accessCookie))
                .andExpect(status().isOk());

        verify(groupInterviewAnswerService).uploadVideo(1L, 123L, USER_ID, file);
    }

    @Test
    void groupDashboardApisUseAuthenticatedUserId() throws Exception {
        when(groupInterviewDashboardService.getMyHistory(USER_ID)).thenReturn(List.of());
        when(groupInterviewDashboardService.getMyAnswers(1L, USER_ID)).thenReturn(List.of());

        mockMvc.perform(get("/api/group-interviews/me/history").cookie(accessCookie))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/group-interviews/1/answers/me").cookie(accessCookie))
                .andExpect(status().isOk());

        verify(groupInterviewDashboardService).getMyHistory(USER_ID);
        verify(groupInterviewDashboardService).getMyAnswers(1L, USER_ID);
    }

    @Test
    void groupComparisonApisUseAuthenticatedUserId() throws Exception {
        var overall = comparisonResponse(GroupComparisonScope.OVERALL, null, "종합 강점", "종합 개선");
        var question = comparisonResponse(GroupComparisonScope.QUESTION, 0, "질문 강점", "질문 개선");
        when(groupInterviewComparisonService.getOverall(1L, USER_ID)).thenReturn(List.of(overall));
        when(groupInterviewComparisonService.getQuestion(1L, 0, USER_ID)).thenReturn(List.of(question));

        mockMvc.perform(get("/api/group-interviews/1/comparisons").cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].strengthSummary").value("종합 강점"))
                .andExpect(jsonPath("$[0].improvementSummary").value("종합 개선"));
        mockMvc.perform(get("/api/group-interviews/1/comparisons/questions/0").cookie(accessCookie))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].participantUserId").value(3))
                .andExpect(jsonPath("$[0].strengthSummary").value("질문 강점"))
                .andExpect(jsonPath("$[0].improvementSummary").value("질문 개선"));

        verify(groupInterviewComparisonService).getOverall(1L, USER_ID);
        verify(groupInterviewComparisonService).getQuestion(1L, 0, USER_ID);
    }

    private GroupInterviewComparisonResponse comparisonResponse(
            GroupComparisonScope scope, Integer questionIndex,
            String strength, String improvement) {
        return GroupInterviewComparisonResponse.builder()
                .reportId(1L).roomId(1L).participantUserId(3L).scope(scope)
                .questionIndex(questionIndex).contentScore(new BigDecimal("85.00"))
                .speechScore(new BigDecimal("76.00")).gazeScore(new BigDecimal("88.00"))
                .poseScore(new BigDecimal("80.00")).blinkScore(new BigDecimal("74.00"))
                .totalScore(new BigDecimal("82.00")).rank(1).participantCount(2)
                .strengthSummary(strength).improvementSummary(improvement)
                .status(GroupComparisonStatus.DONE).build();
    }

    @Test
    void groupDashboardApisWithoutCookieReturn401() throws Exception {
        mockMvc.perform(get("/api/group-interviews/me/history"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/group-interviews/1/answers/me"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/group-interviews/1/comparisons"))
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
