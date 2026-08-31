package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupRoomEvent;
import com.example.interviewfitbe.domain.group.entity.*;
import com.example.interviewfitbe.domain.group.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

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
        ReflectionTestUtils.setField(service, "answerStartGraceSeconds", 60L);
        lenient().when(sessionRepository.findByInterviewIdForUpdate(anyLong()))
                .thenAnswer(invocation -> sessionRepository.findByInterviewId(invocation.getArgument(0)));
        lenient().when(sessionRepository.findByIdForUpdate(anyLong()))
                .thenAnswer(invocation -> sessionRepository.findById(invocation.getArgument(0)));
        when(questionRepository.findFirstByRoundNumberAndQuestionText(
                1,
                GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION
        )).thenReturn(Optional.of(question(1L, GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION)));
    }

    @Test
    void firstRoundAlwaysReturnsSelfIntroductionQuestion() {
        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder()
                .interview(interview)
                .build();
        session.start();
        GroupQuestionEntity selfIntroduction = question(
                100L,
                GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION
        );

        when(sessionRepository.findByInterviewId(10L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L))
                .thenReturn(List.of(participant(5L)));
        when(questionRepository.findAllByRoundNumberOrderByIdAsc(1))
                .thenReturn(List.of(question(200L, "본인의 가장 큰 강점은 무엇입니까?")));
        when(questionRepository.findFirstByRoundNumberAndQuestionText(
                1,
                GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION
        )).thenReturn(Optional.of(selfIntroduction));

        var response = service.getSession(10L);

        assertThat(response.getCurrentRound()).isEqualTo(1);
        assertThat(response.getCurrentQuestionId()).isEqualTo(100L);
        assertThat(response.getCurrentQuestionText())
                .isEqualTo(GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION);
        verify(questionRepository, never()).findAllByRoundNumberOrderByIdAsc(1);
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
        when(questionRepository.findFirstByRoundNumberAndQuestionText(
                1,
                GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION
        )).thenReturn(Optional.of(question));
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
        when(questionRepository.findAllByRoundNumberOrderByIdAsc(1)).thenReturn(List.of());

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
        when(questionRepository.findAllByRoundNumberOrderByIdAsc(1)).thenReturn(List.of());

        service.completeExpiredAnswer(99L);

        verify(completionService).complete(
                eq(123L),
                eq(GroupAnswerCompletionType.TIMED_OUT),
                any()
        );
        assertThat(session.getCurrentParticipantIndex()).isEqualTo(1);
    }

    @Test
    void recordingFailureBeforeAnswerStartSkipsCurrentParticipant() {
        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder()
                .id(99L)
                .interview(interview)
                .build();
        session.start();

        when(sessionRepository.findByInterviewId(10L)).thenReturn(Optional.of(session));
        when(sessionRepository.findById(99L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L))
                .thenReturn(List.of(participant(5L), participant(6L)));
        when(participantRepository.countByRoomId(10L)).thenReturn(2L);
        when(answerRepository.saveAndFlush(any())).thenReturn(
                GroupInterviewAnswerEntity.builder().id(123L).build()
        );

        service.skipAnswer(10L, 5L, GroupAnswerCompletionType.RECORDING_FAILED);

        verify(completionService).complete(
                eq(123L),
                eq(GroupAnswerCompletionType.RECORDING_FAILED),
                any()
        );
        assertThat(session.getCurrentParticipantIndex()).isEqualTo(1);
        assertThat(session.getStatus()).isEqualTo(InterviewStatus.QUESTION_VIEW);
        assertThat(session.getCurrentAnswerId()).isNull();

        // skip 직후 늦게 도착한 timeout은 상태를 다시 전환하지 않는다.
        service.completeExpiredAnswer(99L);
        assertThat(session.getCurrentParticipantIndex()).isEqualTo(1);
        verify(completionService, times(1)).complete(anyLong(), any(), any());
    }

    @Test
    void onlyCurrentParticipantCanSkipAnswer() {
        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder()
                .interview(interview)
                .build();
        session.start();

        when(sessionRepository.findByInterviewId(10L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L))
                .thenReturn(List.of(participant(5L), participant(6L)));

        assertThatThrownBy(() -> service.skipAnswer(
                10L,
                6L,
                GroupAnswerCompletionType.SKIPPED
        )).isInstanceOf(org.springframework.security.access.AccessDeniedException.class);

        verifyNoInteractions(answerRepository, completionService);
        assertThat(session.getCurrentParticipantIndex()).isZero();
    }

    @Test
    void missingStartRequestIsAutomaticallySkippedAfterGracePeriod() {
        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder()
                .id(99L)
                .interview(interview)
                .answerStartDeadlineAt(java.time.Instant.now().minusSeconds(1))
                .build();
        session.start();

        when(sessionRepository.findById(99L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L))
                .thenReturn(List.of(participant(5L), participant(6L)));
        when(participantRepository.countByRoomId(10L)).thenReturn(2L);
        when(answerRepository.saveAndFlush(any())).thenReturn(
                GroupInterviewAnswerEntity.builder().id(123L).build()
        );

        service.skipExpiredAnswerStart(99L);

        verify(completionService).complete(
                eq(123L),
                eq(GroupAnswerCompletionType.RECORDING_FAILED),
                any()
        );
        assertThat(session.getCurrentParticipantIndex()).isEqualTo(1);
        assertThat(session.getStatus()).isEqualTo(InterviewStatus.QUESTION_VIEW);
        assertThat(session.getAnswerStartDeadlineAt()).isAfter(java.time.Instant.now());
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
        when(questionRepository.findAllByRoundNumberOrderByIdAsc(1)).thenReturn(List.of());
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

    @Test
    void twoParticipantsCompleteAllFourQuestionsBeforeInterviewFinishes() {
        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        when(interview.getJobRoleId()).thenReturn(2L);
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder()
                .interview(interview)
                .build();
        session.start();

        GroupQuestionEntity firstQuestion = question(31L, "질문 1");
        GroupQuestionEntity secondQuestion = question(32L, "질문 2");
        GroupQuestionEntity thirdQuestion = question(33L, "질문 3");
        GroupQuestionEntity fourthQuestion = question(34L, "질문 4");
        List<GroupParticipantEntity> participants = List.of(participant(5L), participant(6L));
        AtomicLong answerId = new AtomicLong(100L);

        when(sessionRepository.findByInterviewId(10L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L)).thenReturn(participants);
        when(participantRepository.countByRoomId(10L)).thenReturn(2L);
        when(questionRepository.findAllByRoundNumberOrderByIdAsc(1)).thenReturn(List.of(firstQuestion));
        when(questionRepository.findAllByRoundNumberAndJobRoleIsNullOrderByIdAsc(2))
                .thenReturn(List.of(secondQuestion));
        when(questionRepository.findAllByRoundNumberAndJobRoleIsNullOrderByIdAsc(3))
                .thenReturn(List.of(thirdQuestion));
        when(questionRepository.findAllByRoundNumberAndJobRoleIdOrderByIdAsc(4, 2L))
                .thenReturn(List.of(fourthQuestion));
        when(answerRepository.saveAndFlush(any())).thenAnswer(invocation ->
                GroupInterviewAnswerEntity.builder().id(answerId.getAndIncrement()).build()
        );

        assertSessionQuestion(1, 1L, GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION,
                GroupQuestionType.INTRODUCTION, 5L, InterviewStatus.QUESTION_VIEW);

        answer(service, 5L);
        assertProgress(session, 1, 0, 1, InterviewStatus.QUESTION_VIEW);
        assertSessionQuestion(1, 1L, GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION,
                GroupQuestionType.INTRODUCTION, 6L, InterviewStatus.QUESTION_VIEW);
        answer(service, 6L);
        assertProgress(session, 2, 1, 0, InterviewStatus.QUESTION_VIEW);
        assertSessionQuestion(2, 32L, "질문 2", GroupQuestionType.COMMON_COMPETENCY,
                5L, InterviewStatus.QUESTION_VIEW);

        answer(service, 5L);
        assertProgress(session, 2, 1, 1, InterviewStatus.QUESTION_VIEW);
        assertSessionQuestion(2, 32L, "질문 2", GroupQuestionType.COMMON_COMPETENCY,
                6L, InterviewStatus.QUESTION_VIEW);
        answer(service, 6L);
        assertProgress(session, 3, 2, 0, InterviewStatus.QUESTION_VIEW);
        assertSessionQuestion(3, 33L, "질문 3", GroupQuestionType.PRESSURE,
                5L, InterviewStatus.QUESTION_VIEW);

        ArgumentCaptor<GroupRoomEvent> eventCaptor = ArgumentCaptor.forClass(GroupRoomEvent.class);
        verify(messagingTemplate, atLeastOnce()).convertAndSend(eq("/topic/room/10"), eventCaptor.capture());
        GroupRoomEvent questionThreeEvent = eventCaptor.getAllValues().stream()
                .filter(event -> "ANSWER_COMPLETED_QUESTION_CHANGED".equals(event.getType()))
                .filter(event -> Integer.valueOf(3).equals(event.getProgress().getCurrentRound()))
                .findFirst()
                .orElseThrow();
        assertThat(questionThreeEvent.getProgress().getCurrentQuestionIndex()).isEqualTo(2);
        assertThat(questionThreeEvent.getProgress().getCurrentParticipantIndex()).isZero();
        assertThat(questionThreeEvent.getProgress().getStatus()).isEqualTo(InterviewStatus.QUESTION_VIEW);
        assertThat(questionThreeEvent.getProgress().getCurrentQuestionId()).isEqualTo(33L);
        assertThat(questionThreeEvent.getProgress().getCurrentQuestionText()).isEqualTo("질문 3");

        answer(service, 5L);
        assertProgress(session, 3, 2, 1, InterviewStatus.QUESTION_VIEW);
        assertSessionQuestion(3, 33L, "질문 3", GroupQuestionType.PRESSURE,
                6L, InterviewStatus.QUESTION_VIEW);
        answer(service, 6L);
        assertProgress(session, 4, 3, 0, InterviewStatus.QUESTION_VIEW);
        assertSessionQuestion(4, 34L, "질문 4", GroupQuestionType.JOB_SPECIFIC,
                5L, InterviewStatus.QUESTION_VIEW);

        answer(service, 5L);
        assertProgress(session, 4, 3, 1, InterviewStatus.QUESTION_VIEW);
        assertSessionQuestion(4, 34L, "질문 4", GroupQuestionType.JOB_SPECIFIC,
                6L, InterviewStatus.QUESTION_VIEW);
        answer(service, 6L);
        assertProgress(session, 4, 3, 1, InterviewStatus.FINISHED);
        assertSessionQuestion(4, 34L, "질문 4", GroupQuestionType.JOB_SPECIFIC,
                6L, InterviewStatus.FINISHED);
        verify(interview).finishInterview();
    }

    @ParameterizedTest
    @ValueSource(longs = {1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L})
    void fourthRoundResolvesQuestionForEveryJobRole(Long jobRoleId) {
        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getId()).thenReturn(10L);
        when(interview.getJobRoleId()).thenReturn(jobRoleId);
        GroupInterviewSessionEntity session = GroupInterviewSessionEntity.builder()
                .interview(interview)
                .build();
        moveToFourthRound(session);

        long questionId = 300L + jobRoleId;
        GroupQuestionEntity fourthQuestion = question(questionId, "직무 " + jobRoleId + " 질문");
        when(sessionRepository.findByInterviewId(10L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L))
                .thenReturn(List.of(participant(5L), participant(6L)));
        when(questionRepository.findAllByRoundNumberAndJobRoleIdOrderByIdAsc(4, jobRoleId))
                .thenReturn(List.of(fourthQuestion));

        var response = service.getSession(10L);

        assertThat(response.getCurrentRound()).isEqualTo(4);
        assertThat(response.getCurrentQuestionIndex()).isEqualTo(3);
        assertThat(response.getCurrentQuestionId()).isEqualTo(questionId);
        assertThat(response.getCurrentQuestionText()).isEqualTo("직무 " + jobRoleId + " 질문");
        verify(questionRepository)
                .findAllByRoundNumberAndJobRoleIdOrderByIdAsc(4, jobRoleId);
    }

    private void answer(GroupInterviewFlowService service, Long userId) {
        service.startAnswer(10L, userId);
        service.completeAnswer(10L, userId);
    }

    private void moveToFourthRound(GroupInterviewSessionEntity session) {
        session.start();
        for (int completedAnswers = 0; completedAnswers < 6; completedAnswers++) {
            var now = java.time.Instant.now();
            session.startAnswer(now, now.plusSeconds(60));
            session.completeCurrentAnswer(2);
        }
    }

    private void assertProgress(
            GroupInterviewSessionEntity session,
            int round,
            int questionIndex,
            int participantIndex,
            InterviewStatus status
    ) {
        assertThat(session.getCurrentRound()).isEqualTo(round);
        assertThat(session.getCurrentQuestionIndex()).isEqualTo(questionIndex);
        assertThat(session.getCurrentParticipantIndex()).isEqualTo(participantIndex);
        assertThat(session.getStatus()).isEqualTo(status);
    }

    private void assertSessionQuestion(
            int round,
            Long questionId,
            String questionText,
            GroupQuestionType questionType,
            Long participantUserId,
            InterviewStatus status
    ) {
        var response = service.getSession(10L);
        assertThat(response.getCurrentRound()).isEqualTo(round);
        assertThat(response.getCurrentQuestionIndex()).isEqualTo(round - 1);
        assertThat(response.getCurrentQuestionId()).isEqualTo(questionId);
        assertThat(response.getCurrentQuestionText()).isEqualTo(questionText);
        assertThat(response.getQuestionType()).isEqualTo(questionType);
        assertThat(response.getCurrentParticipantUserId()).isEqualTo(participantUserId);
        assertThat(response.getStatus()).isEqualTo(status);
    }

    private GroupQuestionEntity question(Long id, String text) {
        return GroupQuestionEntity.builder()
                .id(id)
                .questionText(text)
                .build();
    }

    private GroupParticipantEntity participant(Long userId) {
        return GroupParticipantEntity.builder()
                .roomId(10L)
                .userId(userId)
                .status("READY")
                .build();
    }
}
