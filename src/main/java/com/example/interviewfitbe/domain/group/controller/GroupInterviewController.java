package com.example.interviewfitbe.domain.group.controller;

import com.example.interviewfitbe.domain.group.dto.GroupInterviewSessionResponse;
import com.example.interviewfitbe.domain.group.dto.GroupAnswerSkipRequest;
import com.example.interviewfitbe.domain.group.entity.GroupAnswerCompletionType;
import com.example.interviewfitbe.domain.group.dto.GroupParticipantResponse;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewAnswerResponse;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewAnswerAnalysisResponse;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewHistoryResponse;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewMyAnswerResponse;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewComparisonResponse;
import com.example.interviewfitbe.domain.group.dto.RoomCreateRequest;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewSessionEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.service.GroupInterviewFlowService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewAnswerService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewAnswerAnalysisService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewDashboardService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewComparisonService;
import com.example.interviewfitbe.domain.livekit.dto.LiveKitTokenResponse;
import com.example.interviewfitbe.domain.livekit.service.LiveKitService;
import com.example.interviewfitbe.global.jwt.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/group-interviews")
@RequiredArgsConstructor
public class GroupInterviewController {


    private final GroupInterviewService groupInterviewService;

    /**
     * 면접 진행 상태 관리 서비스
     */
    private final GroupInterviewFlowService groupInterviewFlowService;

    private final LiveKitService liveKitService;

    private final GroupInterviewAnswerService groupInterviewAnswerService;

    private final GroupInterviewAnswerAnalysisService groupInterviewAnswerAnalysisService;

    private final GroupInterviewDashboardService groupInterviewDashboardService;

    private final GroupInterviewComparisonService groupInterviewComparisonService;



    /**
     * 다대일 면접 방 생성
     */
    @PostMapping
    public ResponseEntity<GroupInterviewEntity> createRoom(
            @RequestBody RoomCreateRequest request) {

        Long userId = AuthUtils.getCurrentUserId();


        // 최대 참여 인원 검증
        if (request.getMaxParticipants() == null
                || request.getMaxParticipants() < 2
                || request.getMaxParticipants() > 4) {


            throw new IllegalArgumentException(
                    "다대일 면접 인원 제한은 최소 2명부터 최대 4명까지 설정 가능합니다."
            );
        }


        GroupInterviewEntity room =
                groupInterviewService.createRoom(
                        request.getTitle(),
                        userId,
                        request.getJobRoleId(),
                        request.getMaxParticipants(),
                        request.getScheduledStartTime()
                );


        return ResponseEntity.ok(room);
    }




    /**
     * 면접 방 입장
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<GroupParticipantResponse> joinRoom(
            @PathVariable Long roomId) {

        Long userId = AuthUtils.getCurrentUserId();


        return ResponseEntity.ok(groupInterviewService.joinRoom(roomId, userId));
    }




    /**
     * 방 참여자 목록 조회
     */
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<List<GroupParticipantResponse>> getParticipants(
            @PathVariable Long roomId) {


        return ResponseEntity.ok(
                groupInterviewService.getParticipants(roomId)
        );
    }

    @PostMapping("/{roomId}/ready")
    public ResponseEntity<GroupParticipantResponse> ready(
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(
                groupInterviewService.setReady(
                        roomId,
                        AuthUtils.getCurrentUserId(),
                        true
                )
        );
    }

    @DeleteMapping("/{roomId}/ready")
    public ResponseEntity<GroupParticipantResponse> unready(
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(
                groupInterviewService.setReady(
                        roomId,
                        AuthUtils.getCurrentUserId(),
                        false
                )
        );
    }

    @DeleteMapping("/{roomId}/participants/me")
    public ResponseEntity<Void> leaveRoom(@PathVariable Long roomId) {
        groupInterviewService.leaveRoom(roomId, AuthUtils.getCurrentUserId());
        return ResponseEntity.noContent().build();
    }

    /**
     * 다대일 면접 참가자의 LiveKit 입장 토큰 발급
     */
    @PostMapping("/{roomId}/livekit-token")
    public ResponseEntity<LiveKitTokenResponse> createLiveKitToken(
            @PathVariable Long roomId) {

        Long userId = AuthUtils.getCurrentUserId();

        return ResponseEntity.ok(
                liveKitService.createGroupInterviewToken(roomId, userId)
        );
    }




    /**
     * 면접 방 목록 조회
     */
    @GetMapping
    public ResponseEntity<List<GroupInterviewEntity>> getRooms() {


        return ResponseEntity.ok(
                groupInterviewService.getAllRooms()
        );
    }




    /**
     * 면접 방 상세 조회
     */
    @GetMapping("/{roomId}")
    public ResponseEntity<GroupInterviewEntity> getRoom(
            @PathVariable Long roomId) {


        return ResponseEntity.ok(
                groupInterviewService.getRoomById(roomId)
        );
    }





    /**
     * 면접 시작
     *
     * 기존:
     * groupInterviewService.startInterview(roomId)
     *
     * 변경:
     * 면접 세션 생성 + 진행 상태 초기화
     */
    @PostMapping("/{roomId}/start")
    public ResponseEntity<GroupInterviewSessionResponse> startInterview(
            @PathVariable Long roomId) {

        Long userId = AuthUtils.getCurrentUserId();
        groupInterviewFlowService.start(roomId, userId);
        return ResponseEntity.ok(groupInterviewFlowService.getSession(roomId));
    }


    /**
     * 답변 시작
     */
    @PostMapping("/{roomId}/answer/start")
    public ResponseEntity<GroupInterviewSessionResponse> startAnswer(
            @PathVariable Long roomId) {
        groupInterviewFlowService.startAnswer(roomId, AuthUtils.getCurrentUserId());
        return ResponseEntity.ok(groupInterviewFlowService.getSession(roomId));
    }


    /**
     * 답변 종료
     */
    @PostMapping({"/{roomId}/answer-complete", "/{roomId}/answer/end"})
    public ResponseEntity<GroupInterviewSessionResponse> completeAnswer(
            @PathVariable Long roomId) {
        groupInterviewFlowService.completeAnswer(roomId, AuthUtils.getCurrentUserId());
        return ResponseEntity.ok(groupInterviewFlowService.getSession(roomId));
    }

    @PostMapping("/{roomId}/answer/skip")
    public ResponseEntity<GroupInterviewSessionResponse> skipAnswer(
            @PathVariable Long roomId,
            @RequestBody(required = false) GroupAnswerSkipRequest request
    ) {
        groupInterviewFlowService.skipAnswer(
                roomId,
                AuthUtils.getCurrentUserId(),
                request == null
                        ? GroupAnswerCompletionType.SKIPPED
                        : request.resolvedCompletionType()
        );
        return ResponseEntity.ok(groupInterviewFlowService.getSession(roomId));
    }

    /**
     * 면접 종료
     */
    @PostMapping("/{roomId}/finish")
    public ResponseEntity<GroupInterviewSessionResponse> finishInterview(
            @PathVariable Long roomId) {
        groupInterviewFlowService.finish(roomId, AuthUtils.getCurrentUserId());
        return ResponseEntity.ok(groupInterviewFlowService.getSession(roomId));
    }

    @GetMapping("/{roomId}/session")
    public ResponseEntity<GroupInterviewSessionResponse> getSession(
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(groupInterviewFlowService.getSession(roomId));
    }

    @PostMapping(value = "/{roomId}/answers/{answerId}/video", consumes = "multipart/form-data")
    public ResponseEntity<GroupInterviewAnswerResponse> uploadAnswerVideo(
            @PathVariable Long roomId,
            @PathVariable Long answerId,
            @RequestPart("file") MultipartFile file
    ) throws java.io.IOException {
        return ResponseEntity.ok(groupInterviewAnswerService.uploadVideo(
                roomId,
                answerId,
                AuthUtils.getCurrentUserId(),
                file
        ));
    }

    @GetMapping("/{roomId}/answers/{answerId}/video")
    public ResponseEntity<StreamingResponseBody> streamMyAnswerVideo(
            @PathVariable Long roomId,
            @PathVariable Long answerId,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        return groupInterviewAnswerService.streamMyVideo(
                roomId, answerId, AuthUtils.getCurrentUserId(), rangeHeader);
    }

    @GetMapping("/{roomId}/answers")
    public ResponseEntity<List<GroupInterviewAnswerResponse>> getAnswers(
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(groupInterviewAnswerService.getAnswers(
                roomId,
                AuthUtils.getCurrentUserId()
        ));
    }

    @PostMapping("/{roomId}/answers/{answerId}/analysis")
    public ResponseEntity<GroupInterviewAnswerAnalysisResponse> requestAnswerAnalysis(
            @PathVariable Long roomId,
            @PathVariable Long answerId
    ) {
        return ResponseEntity.accepted().body(
                groupInterviewAnswerAnalysisService.requestAnalysis(
                        roomId,
                        answerId,
                        AuthUtils.getCurrentUserId()
                )
        );
    }

    @GetMapping("/{roomId}/answers/{answerId}/analysis")
    public ResponseEntity<GroupInterviewAnswerAnalysisResponse> getAnswerAnalysis(
            @PathVariable Long roomId,
            @PathVariable Long answerId
    ) {
        return ResponseEntity.ok(
                groupInterviewAnswerAnalysisService.getAnalysis(
                        roomId,
                        answerId,
                        AuthUtils.getCurrentUserId()
                )
        );
    }

    @GetMapping("/me/history")
    public ResponseEntity<List<GroupInterviewHistoryResponse>> getMyHistory() {
        return ResponseEntity.ok(groupInterviewDashboardService.getMyHistory(
                AuthUtils.getCurrentUserId()
        ));
    }

    @GetMapping("/{roomId}/answers/me")
    public ResponseEntity<List<GroupInterviewMyAnswerResponse>> getMyAnswers(
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(groupInterviewDashboardService.getMyAnswers(
                roomId,
                AuthUtils.getCurrentUserId()
        ));
    }

    @GetMapping("/{roomId}/comparisons")
    public ResponseEntity<List<GroupInterviewComparisonResponse>> getOverallComparisons(
            @PathVariable Long roomId
    ) {
        return ResponseEntity.ok(groupInterviewComparisonService.getOverall(
                roomId, AuthUtils.getCurrentUserId()));
    }

    @GetMapping("/{roomId}/comparisons/questions/{questionIndex}")
    public ResponseEntity<List<GroupInterviewComparisonResponse>> getQuestionComparisons(
            @PathVariable Long roomId,
            @PathVariable Integer questionIndex
    ) {
        return ResponseEntity.ok(groupInterviewComparisonService.getQuestion(
                roomId, questionIndex, AuthUtils.getCurrentUserId()));
    }

}
