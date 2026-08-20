package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupRoomEvent;
import com.example.interviewfitbe.domain.group.entity.*;
import com.example.interviewfitbe.domain.group.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GroupInterviewFlowServiceTest {

    private GroupInterviewSessionRepository sessionRepository;
    private GroupParticipantRepository participantRepository;
    private GroupQuestionRepository questionRepository;
    private GroupInterviewAnswerRepository answerRepository;
    private GroupInterviewAnswerCompletionService completionService;
    private SimpMessagingTemplate messagingTemplate;
    private GroupInterviewFlowService service;

    @BeforeEach
    void setUp() {
        sessionRepository = mock(GroupInterviewSessionRepository.class);
        participantRepository = mock(GroupParticipantRepository.class);
        questionRepository = mock(GroupQuestionRepository.class);
        answerRepository = mock(GroupInterviewAnswerRepository.class);
        completionService = mock(GroupInterviewAnswerCompletionService.class);
        messagingTemplate = mock(SimpMessagingTemplate.class);
        service = new GroupInterviewFlowService(
                mock(GroupInterviewRepository.class),
                sessionRepository,
                participantRepository,
                questionRepository,
                answerRepository,
                completionService,
                messagingTemplate
        );
        ReflectionTestUtils.setField(service, "answerLimitSeconds", 60L);
    }

    @Test
    void startAnswerCreatesServerAnswerIdAndBroadcastsIt() {
        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder()
                .interview(interview)
                .build();
        session.start();
        GroupQuestionEntity question = mock(GroupQuestionEntity.class);
        when(question.getId()).thenReturn(30L);

        when(sessionRepository.findByInterviewId(10L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L))
                .thenReturn(List.of(participant(5L)));
        when(questionRepository.findFirstByRoundNumberOrderByIdAsc(1))
                .thenReturn(Optional.of(question));
        when(answerRepository.saveAndFlush(any())).thenReturn(
                GroupInterviewAnswerEntity.builder().id(123L).build()
        );

        service.startAnswer(10L, 5L);

        assertThat(session.getCurrentAnswerId()).isEqualTo(123L);
        ArgumentCaptor<GroupRoomEvent> eventCaptor = ArgumentCaptor.forClass(GroupRoomEvent.class);
        verify(messagingTemplate).convertAndSend(eq("/topic/room/10"), eventCaptor.capture());
        assertThat(eventCaptor.getValue().getType()).isEqualTo("ANSWER_STARTED");
        assertThat(eventCaptor.getValue().getProgress().getAnswerId()).isEqualTo(123L);
    }

    @Test
    void normalCompletionMarksCurrentAnswerWithoutBlockingTransition() {
        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder()
                .interview(interview)
                .build();
        session.start();
        session.startAnswer(java.time.Instant.now(), java.time.Instant.now().plusSeconds(60));
        session.assignCurrentAnswer(123L);

        when(sessionRepository.findByInterviewId(10L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L))
                .thenReturn(List.of(participant(5L), participant(6L)));
        when(participantRepository.countByRoomId(10L)).thenReturn(2L);
        when(questionRepository.findFirstByRoundNumberOrderByIdAsc(1))
                .thenReturn(Optional.empty());

        service.completeAnswer(10L, 5L);

        assertThat(session.getCurrentParticipantIndex()).isEqualTo(1);
        assertThat(session.getStatus()).isEqualTo(InterviewStatus.QUESTION_VIEW);
        verify(completionService).complete(
                eq(123L),
                eq(GroupAnswerCompletionType.COMPLETED),
                any()
        );
    }

    @Test
    void timeoutMarksCurrentAnswerAsTimedOut() {
        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder()
                .id(99L)
                .interview(interview)
                .build();
        session.start();
        session.startAnswer(
                java.time.Instant.now().minusSeconds(120),
                java.time.Instant.now().minusSeconds(60)
        );
        session.assignCurrentAnswer(123L);

        when(sessionRepository.findById(99L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L))
                .thenReturn(List.of(participant(5L), participant(6L)));
        when(participantRepository.countByRoomId(10L)).thenReturn(2L);
        when(questionRepository.findFirstByRoundNumberOrderByIdAsc(1))
                .thenReturn(Optional.empty());

        service.completeExpiredAnswer(99L);

        verify(completionService).complete(
                eq(123L),
                eq(GroupAnswerCompletionType.TIMED_OUT),
                any()
        );
        assertThat(session.getCurrentParticipantIndex()).isEqualTo(1);
    }

    @Test
    void answerCompletionFailurePreventsInterviewTransition() {
        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder()
                .interview(interview)
                .build();
        session.start();
        session.startAnswer(java.time.Instant.now(), java.time.Instant.now().plusSeconds(60));
        session.assignCurrentAnswer(123L);

        when(sessionRepository.findByInterviewId(10L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L))
                .thenReturn(List.of(participant(5L), participant(6L)));
        when(participantRepository.countByRoomId(10L)).thenReturn(2L);
        when(questionRepository.findFirstByRoundNumberOrderByIdAsc(1))
                .thenReturn(Optional.empty());
        doThrow(new IllegalStateException("db unavailable"))
                .when(completionService).complete(anyLong(), any(), any());

        assertThatThrownBy(() -> service.completeAnswer(10L, 5L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("db unavailable");

        assertThat(session.getCurrentParticipantIndex()).isZero();
        assertThat(session.getStatus()).isEqualTo(InterviewStatus.ANSWERING);
        assertThat(session.getCurrentAnswerId()).isEqualTo(123L);
        verify(sessionRepository, never()).saveAndFlush(session);
        verifyNoInteractions(messagingTemplate);
    }

    private GroupParticipantEntity participant(Long userId) {
        return GroupParticipantEntity.builder()
                .roomId(10L)
                .userId(userId)
                .status("READY")
                .build();
    }
}
