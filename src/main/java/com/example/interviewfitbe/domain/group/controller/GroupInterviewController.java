package com.example.interviewfitbe.domain.group.controller;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.service.GroupInterviewService;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/group-interviews")
@RequiredArgsConstructor
public class GroupInterviewController {

    private final GroupInterviewService groupInterviewService;

    /**
     * POST /api/group-interviews
     * 다대일 면접 방 생성 API
     */
    @PostMapping
    public ResponseEntity<GroupInterviewEntity> createRoom(@RequestBody CreateRoomRequest request) {
        GroupInterviewEntity room = groupInterviewService.createRoom(request.getHostUserId(), request.getJobRoleId());
        return ResponseEntity.ok(room);
    }

    /**
     * POST /api/group-interviews/{roomId}/join
     * 면접 방 입장(참여) API
     */
    @PostMapping("/{roomId}/join")
    public ResponseEntity<GroupParticipantEntity> joinRoom(
            @PathVariable Long roomId,
            @RequestBody JoinRoomRequest request) {
        GroupParticipantEntity participant = groupInterviewService.joinRoom(roomId, request.getUserId());
        return ResponseEntity.ok(participant);
    }

    /**
     * GET /api/group-interviews/{roomId}/participants
     * 방 참여자 목록 조회 API
     */
    @GetMapping("/{roomId}/participants")
    public ResponseEntity<List<GroupParticipantEntity>> getParticipants(@PathVariable Long roomId) {
        List<GroupParticipantEntity> participants = groupInterviewService.getParticipants(roomId);
        return ResponseEntity.ok(participants);
    }

    // --- API 요청용 내부 DTO 클래스들 ---
    @Getter
    @Setter
    public static class CreateRoomRequest {
        private Long hostUserId;
        private Long jobRoleId;
    }

    @Getter
    @Setter
    public static class JoinRoomRequest {
        private Long userId;
    }
}