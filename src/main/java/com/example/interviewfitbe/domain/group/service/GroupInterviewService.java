package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupInterviewService {

    private final GroupInterviewRepository groupInterviewRepository;
    private final GroupParticipantRepository groupParticipantRepository;


    /**
     * 다대일 면접 방 생성
     */
    public GroupInterviewEntity createRoom(
            String title,
            Long hostUserId,
            Long jobRoleId,
            Integer maxParticipants,
            LocalDateTime scheduledStartTime) {

        // 최대 참여 인원 검증 (2~4명)
        if (maxParticipants == null
                || maxParticipants < 2
                || maxParticipants > 4) {

            throw new IllegalArgumentException(
                    "다대일 면접 인원 제한은 최소 2명부터 최대 4명까지 설정 가능합니다."
            );
        }


        // 면접 방 생성
        GroupInterviewEntity room =
                GroupInterviewEntity.builder()
                        .title(title)
                        .hostUserId(hostUserId)
                        .jobRoleId(jobRoleId)
                        .maxParticipants(maxParticipants)
                        .scheduledStartTime(scheduledStartTime)

                        // LiveKit 연결용 Room 이름 생성
                        .livekitRoomName(
                                "interview-room-" + UUID.randomUUID()
                        )

                        .roomStatus("WAITING")
                        .build();


        GroupInterviewEntity savedRoom =
                groupInterviewRepository.save(room);


        // 방 생성자는 자동 참여
        GroupParticipantEntity hostParticipant =
                GroupParticipantEntity.builder()
                        .roomId(savedRoom.getId())
                        .userId(hostUserId)
                        .status("JOINED")
                        .build();


        groupParticipantRepository.save(hostParticipant);


        return savedRoom;
    }


    /**
     * 면접 방 입장
     */
    public GroupParticipantEntity joinRoom(
            Long roomId,
            Long userId) {

        GroupInterviewEntity room =
                groupInterviewRepository.findById(roomId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 면접 방입니다. ID: " + roomId
                                ));


        // 대기 상태가 아니면 입장 불가
        if (!"WAITING".equals(room.getRoomStatus())) {
            throw new IllegalStateException(
                    "이미 진행 중이거나 종료된 면접 방입니다."
            );
        }


        // 최대 인원 확인
        long participantCount =
                groupParticipantRepository.countByRoomId(roomId);


        if (participantCount >= room.getMaxParticipants()) {
            throw new IllegalStateException(
                    "이미 최대 인원이 참여한 면접 방입니다."
            );
        }


        // 중복 참여 확인
        if (groupParticipantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new IllegalStateException(
                    "이미 참여 중인 사용자입니다."
            );
        }


        GroupParticipantEntity participant =
                GroupParticipantEntity.builder()
                        .roomId(roomId)
                        .userId(userId)
                        .status("JOINED")
                        .build();


        return groupParticipantRepository.save(participant);
    }


    /**
     * 면접 시작
     */
    public void startInterview(Long roomId) {

        GroupInterviewEntity room =
                groupInterviewRepository.findById(roomId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 면접 방입니다."
                                ));

        // 상태 변경 + 실제 시작 시간 저장
        room.startInterview();
    }


    /**
     * 면접 종료
     */
    public void finishInterview(Long roomId) {

        GroupInterviewEntity room =
                groupInterviewRepository.findById(roomId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 면접 방입니다."
                                ));

        // 상태 변경 + 실제 종료 시간 저장
        room.finishInterview();
    }


    /**
     * 참여자 목록 조회
     */
    @Transactional(readOnly = true)
    public List<GroupParticipantEntity> getParticipants(Long roomId) {

        return groupParticipantRepository.findAllByRoomId(roomId);
    }


    /**
     * 면접 방 목록 조회
     */
    @Transactional(readOnly = true)
    public List<GroupInterviewEntity> getAllRooms() {

        return groupInterviewRepository.findAll();
    }


    /**
     * 면접 방 상세 조회
     */
    @Transactional(readOnly = true)
    public GroupInterviewEntity getRoomById(Long roomId) {

        return groupInterviewRepository.findById(roomId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "존재하지 않는 면접 방입니다. ID: " + roomId
                        ));
    }
}