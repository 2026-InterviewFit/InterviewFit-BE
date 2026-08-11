package com.example.interviewfitbe.domain.group.controller;

import com.example.interviewfitbe.domain.group.dto.JoinRoomRequest;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewSessionResponse;
import com.example.interviewfitbe.domain.group.dto.RoomCreateRequest;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewSessionEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.service.GroupInterviewFlowService;
import com.example.interviewfitbe.domain.group.service.GroupInterviewService;
import com.example.interviewfitbe.domain.livekit.dto.LiveKitTokenResponse;
import com.example.interviewfitbe.domain.livekit.service.LiveKitService;
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
                        request.getHostUserId(),
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
    public ResponseEntity<GroupParticipantEntity> joinRoom(
            @PathVariable Long roomId,
            @RequestBody JoinRoomRequest request) {


        return ResponseEntity.ok(
                groupInterviewService.joinRoom(
                        roomId,
                        request.getUserId()
                )
        );
    }




    /**
     * 방 참여자 목록 조회
     */
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<List<GroupParticipantEntity>> getParticipants(
            @PathVariable Long roomId) {


        return ResponseEntity.ok(
                groupInterviewService.getParticipants(roomId)
        );
    }

    /**
     * 다대일 면접 참가자의 LiveKit 입장 토큰 발급
     */
    @PostMapping("/{roomId}/livekit-token")
    public ResponseEntity<LiveKitTokenResponse> createLiveKitToken(
            @PathVariable Long roomId,
            @RequestParam Long userId) {

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
    public ResponseEntity<GroupInterviewSessionEntity> startInterview(
            @PathVariable Long roomId) {


        GroupInterviewSessionEntity session =
                groupInterviewFlowService.start(roomId);


        return ResponseEntity.ok(session);
    }


    /**
     * 답변 시작
     */
    @PostMapping("/{roomId}/answer/start")
    public ResponseEntity<Void> startAnswer(
            @PathVariable Long roomId) {


        groupInterviewFlowService.startAnswer(roomId);


        return ResponseEntity.ok().build();
    }


    /**
     * 답변 종료
     */
    @PostMapping("/{roomId}/answer/end")
    public ResponseEntity<Void> endAnswer(
            @PathVariable Long roomId,
            @RequestParam int participantCount) {


        groupInterviewFlowService.endAnswer(
                roomId,
                participantCount
        );


        return ResponseEntity.ok().build();
    }

    /**
     * 다음 질문 이동
     */
    @PostMapping("/{roomId}/next-question")
    public ResponseEntity<GroupInterviewSessionEntity> nextQuestion(
            @PathVariable Long roomId) {

        GroupInterviewSessionEntity session =
                groupInterviewFlowService.nextQuestion(roomId);



        return ResponseEntity.ok(session);
    }

    /**
     * 다음 라운드 이동
     */
    @PostMapping("/{roomId}/next-round")
    public ResponseEntity<GroupInterviewSessionResponse> nextRound(
            @PathVariable Long roomId) {

        GroupInterviewSessionEntity session =
                groupInterviewFlowService.nextRound(roomId);

        return ResponseEntity.ok(
                GroupInterviewSessionResponse.from(session)
        );
    }

    /**
     * 면접 종료
     */
    @PostMapping("/{roomId}/finish")
    public ResponseEntity<Void> finishInterview(
            @PathVariable Long roomId) {


        groupInterviewFlowService.finish(roomId);


        return ResponseEntity.ok().build();
    }

}
