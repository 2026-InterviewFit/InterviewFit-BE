package com.example.interviewfitbe.domain.group.controller;

import com.example.interviewfitbe.domain.group.dto.GroupInterviewSessionResponse;
import com.example.interviewfitbe.domain.group.dto.GroupParticipantResponse;
import com.example.interviewfitbe.domain.group.dto.RoomCreateRequest;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewSessionEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.service.GroupInterviewFlowService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewService;
import com.example.interviewfitbe.domain.livekit.dto.LiveKitTokenResponse;
import com.example.interviewfitbe.domain.livekit.service.LiveKitService;
import com.example.interviewfitbe.global.jwt.AuthUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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
    @PostMapping("/{roomId}/answer/end")
    public ResponseEntity<GroupInterviewSessionResponse> endAnswer(
            @PathVariable Long roomId) {
        groupInterviewFlowService.endAnswer(roomId, AuthUtils.getCurrentUserId());
        return ResponseEntity.ok(groupInterviewFlowService.getSession(roomId));
    }

    /**
     * 다음 질문 이동
     */
    @PostMapping("/{roomId}/next-question")
    public ResponseEntity<GroupInterviewSessionResponse> nextQuestion(
            @PathVariable Long roomId) {
        groupInterviewFlowService.nextQuestion(roomId, AuthUtils.getCurrentUserId());
        return ResponseEntity.ok(groupInterviewFlowService.getSession(roomId));
    }

    /**
     * 다음 라운드 이동
     */
    @PostMapping("/{roomId}/next-round")
    public ResponseEntity<GroupInterviewSessionResponse> nextRound(
            @PathVariable Long roomId) {

        groupInterviewFlowService.nextRound(roomId, AuthUtils.getCurrentUserId());
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

}
