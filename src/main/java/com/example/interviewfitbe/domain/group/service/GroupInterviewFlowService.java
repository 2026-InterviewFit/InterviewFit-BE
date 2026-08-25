package com.example.interviewfitbe.domain.group.service;


import com.example.interviewfitbe.domain.group.dto.InterviewProgressResponse;
import com.example.interviewfitbe.domain.group.dto.GroupRoomEvent;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewSessionResponse;
import com.example.interviewfitbe.domain.group.entity.*;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewSessionRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import com.example.interviewfitbe.domain.group.repository.GroupQuestionRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;


@Service
@RequiredArgsConstructor
@Slf4j
public class GroupInterviewFlowService {


    private final GroupInterviewRepository groupInterviewRepository;

    private final GroupInterviewSessionRepository sessionRepository;

    private final GroupParticipantRepository groupParticipantRepository;

    private final GroupQuestionRepository groupQuestionRepository;

    private final GroupInterviewAnswerRepository answerRepository;

    private final GroupInterviewAnswerCompletionService answerCompletionService;

    private final SimpMessagingTemplate messagingTemplate;

    @Autowired(required = false)
    private GroupInterviewComparisonService comparisonService;

    @Value("${app.group-interview.answer-limit-seconds:60}")
    private long answerLimitSeconds;



    /**
     * 면접 시작
     */
    @Transactional
    public GroupInterviewSessionEntity start(Long roomId, Long actorUserId){


        GroupInterviewEntity interview =
                groupInterviewRepository.findById(roomId)
                        .orElseThrow();

        validateHost(interview, actorUserId);
        validateAllReady(roomId);

        if (sessionRepository.findByInterviewId(roomId).isPresent()) {
            throw new IllegalStateException("이미 시작된 면접입니다.");
        }


        GroupInterviewSessionEntity session =
                GroupInterviewSessionEntity.builder()
                        .interview(interview)
                        .build();


        session.start();
        interview.startInterview();


        GroupInterviewSessionEntity savedSession = sessionRepository.save(session);

        broadcastProgress("INTERVIEW_STARTED", roomId, actorUserId, savedSession);

        return savedSession;

    }



    /**
     * 답변 시작
     */
    @Transactional
    public void startAnswer(Long roomId, Long actorUserId){
        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        validateCurrentParticipant(roomId, session, actorUserId);
        Instant startedAt = Instant.now();
        session.startAnswer(startedAt, startedAt.plus(answerLimitSeconds, ChronoUnit.SECONDS));
        GroupQuestionEntity question = currentQuestion(session);
        GroupInterviewAnswerEntity answer = answerRepository.saveAndFlush(
                GroupInterviewAnswerEntity.builder()
                        .groupInterview(session.getInterview())
                        .participantUserId(actorUserId)
                        .questionId(question == null ? null : question.getId())
                        .round(session.getCurrentRound())
                        .questionIndex(session.getCurrentQuestionIndex())
                        .answerStartedAt(startedAt)
                        .build()
        );
        session.assignCurrentAnswer(answer.getId());
        sessionRepository.saveAndFlush(session);

        broadcastProgress("ANSWER_STARTED", roomId, actorUserId, session);

    }



    /**
     * 현재 답변자의 답변 완료. 이후 상태는 서버가 결정한다.
     */
    @Transactional
    public void completeAnswer(
            Long roomId,
            Long actorUserId
    ){

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        validateCurrentParticipant(roomId, session, actorUserId);
        completeAnswer(roomId, actorUserId, session, false);

    }

    @Transactional
    public void completeExpiredAnswer(Long sessionId) {
        GroupInterviewSessionEntity session = sessionRepository.findById(sessionId).orElse(null);
        if (session == null
                || session.getStatus() != InterviewStatus.ANSWERING
                || session.getAnswerDeadlineAt() == null
                || session.getAnswerDeadlineAt().isAfter(Instant.now())) {
            return;
        }
        Long roomId = session.getInterview().getId();
        Long timedOutUserId = currentParticipantUserId(roomId, session.getCurrentParticipantIndex());
        completeAnswer(roomId, timedOutUserId, session, true);
    }

    @Transactional(readOnly = true)
    public List<Long> findExpiredAnswerSessionIds(Instant now) {
        return sessionRepository.findExpiredAnswerSessionIds(now);
    }

    /**
     * 면접 종료
     */
    @Transactional
    public void finish(Long roomId, Long actorUserId){

        GroupInterviewEntity interview = groupInterviewRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("면접 방을 찾을 수 없습니다."));
        validateHost(interview, actorUserId);

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        session.finish();
        interview.finishInterview();
        sessionRepository.saveAndFlush(session);

        broadcastProgress("INTERVIEW_FINISHED", roomId, actorUserId, session);
        triggerComparison(roomId);

    }

    private void broadcastProgress(
            String type,
            Long roomId,
            Long actorUserId,
            GroupInterviewSessionEntity session
    ) {
        InterviewProgressResponse progress = toProgress(roomId, session);
        log.info(
                "Group interview WebSocket progress: destination={}, type={}, roomId={}, currentRound={}, currentQuestionIndex={}, currentParticipantIndex={}, status={}",
                "/topic/room/" + roomId,
                type,
                roomId,
                progress.getCurrentRound(),
                progress.getCurrentQuestionIndex(),
                progress.getCurrentParticipantIndex(),
                progress.getStatus()
        );
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                GroupRoomEvent.progress(
                        type,
                        roomId,
                        actorUserId,
                        progress
                )
        );
    }

    @Transactional(readOnly = true)
    public GroupInterviewSessionResponse getSession(Long roomId) {
        GroupInterviewSessionEntity session = sessionRepository.findByInterviewId(roomId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
        var question = currentQuestion(session);
        return GroupInterviewSessionResponse.from(
                session,
                currentParticipantUserId(roomId, session.getCurrentParticipantIndex()),
                question == null ? null : question.getId(),
                question == null ? null : question.getQuestionText()
        );
    }

    private InterviewProgressResponse toProgress(
            Long roomId,
            GroupInterviewSessionEntity session
    ) {
        var question = currentQuestion(session);
        return InterviewProgressResponse.from(
                roomId,
                session,
                currentParticipantUserId(roomId, session.getCurrentParticipantIndex()),
                question == null ? null : question.getId(),
                question == null ? null : question.getQuestionText()
        );
    }

    private GroupQuestionEntity currentQuestion(GroupInterviewSessionEntity session) {
        if (session.getCurrentRound() == null) {
            return null;
        }
        if (session.getCurrentRound() == 3) {
            return groupQuestionRepository
                    .findFirstByRoundNumberAndJobRoleIdOrderByIdAsc(
                            3,
                            session.getInterview().getJobRoleId()
                    )
                    .orElse(null);
        }
        return groupQuestionRepository
                .findFirstByRoundNumberOrderByIdAsc(session.getCurrentRound())
                .orElse(null);
    }

    private Long currentParticipantUserId(Long roomId, Integer index) {
        var participants = groupParticipantRepository.findAllByRoomIdOrderByIdAsc(roomId);
        if (index == null || index < 0 || index >= participants.size()) {
            return null;
        }
        return participants.get(index).getUserId();
    }

    private int participantCount(Long roomId) {
        int count = Math.toIntExact(groupParticipantRepository.countByRoomId(roomId));
        if (count <= 0) {
            throw new IllegalStateException("면접 참가자가 없습니다.");
        }
        return count;
    }

    private void completeAnswer(
            Long roomId,
            Long actorUserId,
            GroupInterviewSessionEntity session,
            boolean timedOut
    ) {
        Long completedAnswerId = session.getCurrentAnswerId();
        answerCompletionService.complete(
                completedAnswerId,
                timedOut ? GroupAnswerCompletionType.TIMED_OUT : GroupAnswerCompletionType.COMPLETED,
                Instant.now()
        );

        GroupInterviewSessionEntity.AnswerCompletionResult result =
                session.completeCurrentAnswer(participantCount(roomId));
        if (result == GroupInterviewSessionEntity.AnswerCompletionResult.INTERVIEW_FINISHED) {
            session.getInterview().finishInterview();
        }
        sessionRepository.saveAndFlush(session);
        log.info(
                "Group interview state saved after answer completion: roomId={}, result={}, currentRound={}, currentQuestionIndex={}, currentParticipantIndex={}, status={}",
                roomId,
                result,
                session.getCurrentRound(),
                session.getCurrentQuestionIndex(),
                session.getCurrentParticipantIndex(),
                session.getStatus()
        );

        String type = switch (result) {
            case NEXT_PARTICIPANT -> timedOut
                    ? "ANSWER_TIMED_OUT_SPEAKER_CHANGED"
                    : "ANSWER_COMPLETED_SPEAKER_CHANGED";
            case NEXT_QUESTION -> timedOut
                    ? "ANSWER_TIMED_OUT_QUESTION_CHANGED"
                    : "ANSWER_COMPLETED_QUESTION_CHANGED";
            case INTERVIEW_FINISHED -> timedOut
                    ? "ANSWER_TIMED_OUT_INTERVIEW_FINISHED"
                    : "ANSWER_COMPLETED_INTERVIEW_FINISHED";
        };
        broadcastProgress(type, roomId, actorUserId, session);
        if (result == GroupInterviewSessionEntity.AnswerCompletionResult.INTERVIEW_FINISHED) {
            triggerComparison(roomId);
        }
    }

    private void triggerComparison(Long roomId) {
        if (comparisonService != null) {
            comparisonService.generateAvailableComparisons(roomId);
        }
    }

    private void validateCurrentParticipant(
            Long roomId,
            GroupInterviewSessionEntity session,
            Long actorUserId
    ) {
        Long currentUserId = currentParticipantUserId(roomId, session.getCurrentParticipantIndex());
        if (currentUserId == null || !currentUserId.equals(actorUserId)) {
            throw new AccessDeniedException("현재 답변자만 자신의 답변 상태를 변경할 수 있습니다.");
        }
    }

    private void validateAllReady(Long roomId) {
        var participants = groupParticipantRepository.findAllByRoomIdOrderByIdAsc(roomId);
        if (participants.size() < 2
                || participants.stream().anyMatch(p -> !"READY".equals(p.getStatus()))) {
            throw new IllegalStateException("최소 2명의 참가자가 모두 준비 완료 상태여야 합니다.");
        }
    }

    private void validateHost(GroupInterviewEntity interview, Long userId) {
        if (!interview.getHostUserId().equals(userId)) {
            throw new AccessDeniedException("방장만 면접 진행 상태를 변경할 수 있습니다.");
        }
    }

}
