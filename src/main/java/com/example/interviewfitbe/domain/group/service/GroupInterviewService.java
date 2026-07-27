package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupInterviewService {

    private final GroupInterviewRepository groupInterviewRepository;
    private final GroupParticipantRepository groupParticipantRepository;

    /**
     * 1. 다대일 면접 방 생성
     */
    public GroupInterviewEntity createRoom(Long hostUserId, Long jobRoleId) {
        GroupInterviewEntity room = GroupInterviewEntity.builder()
                .hostUserId(hostUserId)
                .jobRoleId(jobRoleId)
                .roomStatus("WAITING") // 대기 상태로 시작
                .build();

        GroupInterviewEntity savedRoom = groupInterviewRepository.save(room);

        // 방을 만든 사람(방장)도 참여자 목록에 자동으로 추가
        GroupParticipantEntity hostParticipant = GroupParticipantEntity.builder()
                .roomId(savedRoom.getId())
                .userId(hostUserId)
                .status("JOINED")
                .build();
        groupParticipantRepository.save(hostParticipant);

        return savedRoom;
    }

    /**
     * 2. 사용자가 면접 방에 참여(입장)
     */
    public GroupParticipantEntity joinRoom(Long roomId, Long userId) {
        // 방이 존재하는지 확인
        GroupInterviewEntity room = groupInterviewRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 면접 방입니다. ID: " + roomId));

        // 이미 시작된 방인지 체크
        if (!"WAITING".equals(room.getRoomStatus())) {
            throw new IllegalStateException("이미 진행 중이거나 종료된 면접 방입니다.");
        }

        GroupParticipantEntity participant = GroupParticipantEntity.builder()
                .roomId(roomId)
                .userId(userId)
                .status("JOINED")
                .build();

        return groupParticipantRepository.save(participant);
    }

    /**
     * 3. 면접 시작 처리 (방 상태 변경)
     */
    public void startInterview(Long roomId) {
        GroupInterviewEntity room = groupInterviewRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 면접 방입니다."));
        room.updateStatus("IN_PROGRESS");
    }

    /**
     * 4. 방에 속한 참여자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<GroupParticipantEntity> getParticipants(Long roomId) {
        return groupParticipantRepository.findAllByRoomId(roomId);
    }
}