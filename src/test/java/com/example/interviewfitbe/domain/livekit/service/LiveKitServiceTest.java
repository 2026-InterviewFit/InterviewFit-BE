package com.example.interviewfitbe.domain.livekit.service;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import com.example.interviewfitbe.global.config.LiveKitConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LiveKitServiceTest {

    private static final Long ROOM_ID = 10L;
    private static final Long USER_ID = 20L;

    private GroupInterviewRepository interviewRepository;
    private GroupParticipantRepository participantRepository;
    private LiveKitConfig liveKitConfig;
    private LiveKitService service;

    @BeforeEach
    void setUp() {
        interviewRepository = mock(GroupInterviewRepository.class);
        participantRepository = mock(GroupParticipantRepository.class);
        liveKitConfig = mock(LiveKitConfig.class);
        service = spy(new LiveKitService(
                interviewRepository,
                participantRepository,
                liveKitConfig
        ));

        when(participantRepository.existsByRoomIdAndUserId(ROOM_ID, USER_ID))
                .thenReturn(true);
        when(liveKitConfig.getUrl()).thenReturn("wss://livekit.test");
        doReturn("issued-token").when(service).createToken("room-name", USER_ID);
    }

    @Test
    void waitingParticipantCanReceiveToken() {
        givenRoom("WAITING");

        var response = service.createGroupInterviewToken(ROOM_ID, USER_ID);

        assertThat(response.getUrl()).isEqualTo("wss://livekit.test");
        assertThat(response.getToken()).isEqualTo("issued-token");
    }

    @Test
    void inProgressParticipantCanReceiveReconnectToken() {
        givenRoom("IN_PROGRESS");

        var response = service.createGroupInterviewToken(ROOM_ID, USER_ID);

        assertThat(response.getUrl()).isEqualTo("wss://livekit.test");
        assertThat(response.getToken()).isEqualTo("issued-token");
    }

    @Test
    void completedRoomCannotIssueToken() {
        givenRoom("COMPLETED");

        assertThatThrownBy(() -> service.createGroupInterviewToken(ROOM_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("종료된 면접 방에는 입장할 수 없습니다.");

        verify(service, never()).createToken("room-name", USER_ID);
    }

    private void givenRoom(String roomStatus) {
        GroupInterviewEntity room = GroupInterviewEntity.builder()
                .livekitRoomName("room-name")
                .roomStatus(roomStatus)
                .build();
        when(interviewRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
    }
}
