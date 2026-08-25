package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.entity.GroupParticipantEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class GroupInterviewServiceTest {

    private GroupInterviewRepository interviewRepository;
    private GroupParticipantRepository participantRepository;
    private GroupParticipantJoinWriter joinWriter;
    private JobRoleRepository jobRoleRepository;
    private GroupInterviewService service;

    @BeforeEach
    void setUp() {
        interviewRepository = mock(GroupInterviewRepository.class);
        participantRepository = mock(GroupParticipantRepository.class);
        joinWriter = mock(GroupParticipantJoinWriter.class);
        jobRoleRepository = mock(JobRoleRepository.class);
        service = new GroupInterviewService(
                interviewRepository,
                participantRepository,
                mock(UserRepository.class),
                mock(SimpMessagingTemplate.class),
                joinWriter,
                jobRoleRepository
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

    @Test
    void createRoomRejectsUnknownJobRoleBeforeSavingRoom() {
        when(jobRoleRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createRoom(
                "room", 1L, 999L, 2, java.time.LocalDateTime.now()
        ))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("존재하지 않는 직무 ID");

        verify(interviewRepository, never()).save(any());
        verifyNoInteractions(participantRepository);
    }

    @Test
    void createRoomAcceptsJobRoleFromJobRolesRepository() {
        when(jobRoleRepository.findById(11L)).thenReturn(Optional.of(
                JobRoleEntity.builder().id(11L).category("기타").name("기타").build()
        ));
        when(interviewRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.createRoom("room", 1L, 11L, 2, java.time.LocalDateTime.now());

        verify(interviewRepository).save(argThat(room -> room.getJobRoleId().equals(11L)));
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
