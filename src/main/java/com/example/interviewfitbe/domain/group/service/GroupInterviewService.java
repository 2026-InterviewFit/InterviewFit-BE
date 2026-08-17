package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupParticipantResponse;
import com.example.interviewfitbe.domain.group.dto.GroupRoomEvent;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class GroupInterviewService {

    private final GroupInterviewRepository groupInterviewRepository;
    private final GroupParticipantRepository groupParticipantRepository;
    private final UserRepository userRepository;
    private final SimpMessagingTemplate messagingTemplate;


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
    public GroupParticipantResponse joinRoom(
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


        GroupParticipantEntity savedParticipant = groupParticipantRepository.save(participant);
        broadcastParticipant("PARTICIPANT_JOINED", room, savedParticipant);
        return toResponse(room, savedParticipant);
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
    public List<GroupParticipantResponse> getParticipants(Long roomId) {
        GroupInterviewEntity room = getRoomById(roomId);
        List<GroupParticipantEntity> participants =
                groupParticipantRepository.findAllByRoomIdOrderByIdAsc(roomId);
        Map<Long, UserEntity> users = userRepository.findAllById(
                        participants.stream().map(GroupParticipantEntity::getUserId).toList()
                ).stream()
                .collect(Collectors.toMap(UserEntity::getId, Function.identity()));

        return participants.stream()
                .map(participant -> GroupParticipantResponse.from(
                        participant,
                        users.containsKey(participant.getUserId())
                                ? users.get(participant.getUserId()).getName()
                                : null,
                        room.getHostUserId()
                ))
                .toList();
    }

    public GroupParticipantResponse setReady(Long roomId, Long userId, boolean ready) {
        GroupInterviewEntity room = getWaitingRoom(roomId);
        GroupParticipantEntity participant = getParticipant(roomId, userId);
        if (ready) {
            participant.ready();
        } else {
            participant.unready();
        }
        GroupParticipantEntity saved = groupParticipantRepository.save(participant);
        GroupParticipantResponse response = toResponse(room, saved);
        broadcastParticipant(
                ready ? "PARTICIPANT_READY" : "PARTICIPANT_UNREADY",
                room,
                saved
        );
        return response;
    }

    public void leaveRoom(Long roomId, Long userId) {
        GroupInterviewEntity room = getWaitingRoom(roomId);
        if (room.getHostUserId().equals(userId)) {
            throw new IllegalStateException("방장은 면접 방을 나갈 수 없습니다.");
        }
        GroupParticipantEntity participant = getParticipant(roomId, userId);
        GroupParticipantResponse response = toResponse(room, participant);
        groupParticipantRepository.delete(participant);
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                GroupRoomEvent.participant("PARTICIPANT_LEFT", roomId, userId, response)
        );
    }

    @Transactional(readOnly = true)
    public void validateHost(Long roomId, Long userId) {
        GroupInterviewEntity room = getRoomById(roomId);
        if (!room.getHostUserId().equals(userId)) {
            throw new AccessDeniedException("방장만 면접 진행 상태를 변경할 수 있습니다.");
        }
    }

    @Transactional(readOnly = true)
    public void validateAllParticipantsReady(Long roomId) {
        List<GroupParticipantEntity> participants =
                groupParticipantRepository.findAllByRoomIdOrderByIdAsc(roomId);
        if (participants.isEmpty()
                || participants.stream().anyMatch(p -> !"READY".equals(p.getStatus()))) {
            throw new IllegalStateException("모든 참가자가 준비 완료 상태여야 합니다.");
        }
    }

    private GroupInterviewEntity getWaitingRoom(Long roomId) {
        GroupInterviewEntity room = getRoomById(roomId);
        if (!"WAITING".equals(room.getRoomStatus())) {
            throw new IllegalStateException("대기 중인 면접 방에서만 변경할 수 있습니다.");
        }
        return room;
    }

    private GroupParticipantEntity getParticipant(Long roomId, Long userId) {
        return groupParticipantRepository.findByRoomIdAndUserId(roomId, userId)
                .orElseThrow(() -> new AccessDeniedException("면접 방 참가자가 아닙니다."));
    }

    private GroupParticipantResponse toResponse(
            GroupInterviewEntity room,
            GroupParticipantEntity participant
    ) {
        String name = userRepository.findById(participant.getUserId())
                .map(UserEntity::getName)
                .orElse(null);
        return GroupParticipantResponse.from(participant, name, room.getHostUserId());
    }

    private void broadcastParticipant(
            String type,
            GroupInterviewEntity room,
            GroupParticipantEntity participant
    ) {
        messagingTemplate.convertAndSend(
                "/topic/room/" + room.getId(),
                GroupRoomEvent.participant(
                        type,
                        room.getId(),
                        participant.getUserId(),
                        toResponse(room, participant)
                )
        );
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
