package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GroupInterviewServiceTest {

    private GroupInterviewRepository interviewRepository;
    private GroupParticipantRepository participantRepository;
    private GroupParticipantJoinWriter joinWriter;
    private GroupInterviewService service;

    @BeforeEach
    void setUp() {
        interviewRepository = mock(GroupInterviewRepository.class);
        participantRepository = mock(GroupParticipantRepository.class);
        joinWriter = mock(GroupParticipantJoinWriter.class);
        service = new GroupInterviewService(
                interviewRepository,
                participantRepository,
                mock(UserRepository.class),
                mock(SimpMessagingTemplate.class),
                joinWriter
        );
    }

    @Test
    void repeatedJoinReturnsExistingParticipantWithoutInsert() {
        var room = waitingRoom();
        var participant = participant("JOINED");
        when(interviewRepository.findById(7L)).thenReturn(Optional.of(room));
        when(participantRepository.findFirstByRoomIdAndUserIdOrderByIdAsc(7L, 4L))
                .thenReturn(Optional.of(participant));

        var response = service.joinRoom(7L, 4L);

        assertThat(response.getUserId()).isEqualTo(4L);
        assertThat(response.getStatus()).isEqualTo("JOINED");
        verifyNoInteractions(joinWriter);
    }

    @Test
    void uniqueConstraintRaceReturnsParticipantInsertedByOtherRequest() {
        var room = waitingRoom();
        var participant = participant("JOINED");
        when(interviewRepository.findById(7L)).thenReturn(Optional.of(room));
        when(participantRepository.findFirstByRoomIdAndUserIdOrderByIdAsc(7L, 4L))
                .thenReturn(Optional.empty(), Optional.of(participant));
        when(participantRepository.countByRoomId(7L)).thenReturn(1L);
        when(joinWriter.insert(7L, 4L))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        var response = service.joinRoom(7L, 4L);

        assertThat(response.getUserId()).isEqualTo(4L);
        assertThat(response.getStatus()).isEqualTo("JOINED");
    }

    @Test
    void participantCanTransitionJoinedReadyJoined() {
        var room = waitingRoom();
        var participant = participant("JOINED");
        when(interviewRepository.findById(7L)).thenReturn(Optional.of(room));
        when(participantRepository.findFirstByRoomIdAndUserIdOrderByIdAsc(7L, 4L))
                .thenReturn(Optional.of(participant));
        when(participantRepository.save(participant)).thenReturn(participant);

        assertThat(service.setReady(7L, 4L, true).getStatus()).isEqualTo("READY");
        assertThat(service.setReady(7L, 4L, false).getStatus()).isEqualTo("JOINED");
    }

    private GroupInterviewEntity waitingRoom() {
        return GroupInterviewEntity.builder()
                .hostUserId(1L)
                .maxParticipants(4)
                .roomStatus("WAITING")
                .build();
    }

    private GroupParticipantEntity participant(String status) {
        return GroupParticipantEntity.builder()
                .roomId(7L)
                .userId(4L)
                .status(status)
                .build();
    }
}
