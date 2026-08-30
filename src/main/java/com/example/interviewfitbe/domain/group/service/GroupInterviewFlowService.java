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

    @Value("${app.group-interview.answer-start-grace-seconds:60}")
    private long answerStartGraceSeconds;



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
        scheduleAnswerStartDeadline(session);
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
                sessionRepository.findByInterviewIdForUpdate(roomId)
                        .orElseThrow();


        validateCurrentParticipant(roomId, session, actorUserId);
        Instant startedAt = Instant.now();
        session.startAnswer(startedAt, startedAt.plus(answerLimitSeconds, ChronoUnit.SECONDS));
        GroupInterviewAnswerEntity answer = createAnswer(session, actorUserId, startedAt);
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
                sessionRepository.findByInterviewIdForUpdate(roomId)
                        .orElseThrow();


        validateCurrentParticipant(roomId, session, actorUserId);
        completeAnswer(roomId, actorUserId, session, GroupAnswerCompletionType.COMPLETED);

    }

    /**
     * 녹음을 시작할 수 없거나 답변 도중 미디어 처리가 실패한 현재 참가자를 건너뛴다.
     * QUESTION_VIEW와 ANSWERING 양쪽 상태에서 호출할 수 있다.
     */
    @Transactional
    public void skipAnswer(
            Long roomId,
            Long actorUserId,
            GroupAnswerCompletionType completionType
    ) {
        if (completionType != GroupAnswerCompletionType.SKIPPED
                && completionType != GroupAnswerCompletionType.RECORDING_FAILED) {
            throw new IllegalArgumentException(
                    "skip completionType은 SKIPPED 또는 RECORDING_FAILED만 사용할 수 있습니다."
            );
        }

        GroupInterviewSessionEntity session = sessionRepository
                .findByInterviewIdForUpdate(roomId)
                .orElseThrow(() -> new IllegalArgumentException("면접 세션을 찾을 수 없습니다."));
        validateCurrentParticipant(roomId, session, actorUserId);

        if (session.getStatus() == InterviewStatus.QUESTION_VIEW) {
            Instant now = Instant.now();
            session.startAnswer(now, now);
            GroupInterviewAnswerEntity answer = createAnswer(session, actorUserId, now);
            session.assignCurrentAnswer(answer.getId());
        } else if (session.getStatus() != InterviewStatus.ANSWERING) {
            throw new IllegalStateException("현재 답변을 건너뛸 수 있는 상태가 아닙니다.");
        }

        completeAnswer(roomId, actorUserId, session, completionType);
    }

    @Transactional
    public void completeExpiredAnswer(Long sessionId) {
        GroupInterviewSessionEntity session = sessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null
                || session.getStatus() != InterviewStatus.ANSWERING
                || session.getAnswerDeadlineAt() == null
                || session.getAnswerDeadlineAt().isAfter(Instant.now())) {
            return;
        }
        Long roomId = session.getInterview().getId();
        Long timedOutUserId = currentParticipantUserId(roomId, session.getCurrentParticipantIndex());
        completeAnswer(roomId, timedOutUserId, session, GroupAnswerCompletionType.TIMED_OUT);
    }

    @Transactional(readOnly = true)
    public List<Long> findExpiredAnswerSessionIds(Instant now) {
        return sessionRepository.findExpiredAnswerSessionIds(now);
    }

    @Transactional(readOnly = true)
    public List<Long> findExpiredAnswerStartSessionIds(Instant now) {
        return sessionRepository.findExpiredAnswerStartSessionIds(now);
    }

    @Transactional
    public void skipExpiredAnswerStart(Long sessionId) {
        GroupInterviewSessionEntity session = sessionRepository.findByIdForUpdate(sessionId).orElse(null);
        if (session == null
                || session.getStatus() != InterviewStatus.QUESTION_VIEW
                || session.getAnswerStartDeadlineAt() == null
                || session.getAnswerStartDeadlineAt().isAfter(Instant.now())) {
            return;
        }

        Long roomId = session.getInterview().getId();
        Long participantUserId = currentParticipantUserId(
                roomId,
                session.getCurrentParticipantIndex()
        );
        Instant now = Instant.now();
        session.startAnswer(now, now);
        GroupInterviewAnswerEntity answer = createAnswer(session, participantUserId, now);
        session.assignCurrentAnswer(answer.getId());
        completeAnswer(
                roomId,
                participantUserId,
                session,
                GroupAnswerCompletionType.RECORDING_FAILED
        );
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
                "Group interview progress: destination={}, eventType={}, roomId={}, currentRound={}, currentQuestionIndex={}, currentQuestionId={}, currentQuestionText={}, questionType={}, jobRoleId={}, currentParticipantUserId={}, status={}",
                "/topic/room/" + roomId,
                type,
                roomId,
                progress.getCurrentRound(),
                progress.getCurrentQuestionIndex(),
                progress.getCurrentQuestionId(),
                progress.getCurrentQuestionText(),
                progress.getQuestionType(),
                session.getInterview().getJobRoleId(),
                progress.getCurrentParticipantUserId(),
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
        if (session.getCurrentRound() == 1) {
            return groupQuestionRepository
                    .findFirstByRoundNumberAndQuestionText(
                            1,
                            GroupInterviewQuestionService.SELF_INTRODUCTION_QUESTION
                    )
                    .orElseThrow(() -> new IllegalStateException("자기소개 질문 데이터가 없습니다."));
        }
        int round = session.getCurrentRound();
        List<GroupQuestionEntity> candidates = switch (round) {
            case 2, 3 -> groupQuestionRepository.findAllByRoundNumberAndJobRoleIsNullOrderByIdAsc(round);
            case 4 -> {
                Long jobRoleId = session.getInterview().getJobRoleId();
                if (jobRoleId == null) {
                    throw new IllegalStateException("4라운드 질문 선택에 jobRoleId가 필요합니다.");
                }
                yield groupQuestionRepository.findAllByRoundNumberAndJobRoleIdOrderByIdAsc(4, jobRoleId);
            }
            default -> throw new IllegalStateException("지원하지 않는 면접 라운드입니다: " + round);
        };
        if (candidates.isEmpty()) {
            throw new IllegalStateException(
                    "라운드에 사용할 질문이 없습니다. round=" + round
                            + ", jobRoleId=" + session.getInterview().getJobRoleId()
            );
        }

        long roomId = session.getInterview().getId();
        return GroupQuestionSelector.select(candidates, roomId, round);
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
            GroupAnswerCompletionType completionType
    ) {
        Long completedAnswerId = session.getCurrentAnswerId();
        answerCompletionService.complete(
                completedAnswerId,
                completionType,
                Instant.now()
        );

        GroupInterviewSessionEntity.AnswerCompletionResult result =
                session.completeCurrentAnswer(participantCount(roomId));
        if (session.getStatus() == InterviewStatus.QUESTION_VIEW) {
            scheduleAnswerStartDeadline(session);
        }
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

        String eventPrefix = switch (completionType) {
            case COMPLETED -> "ANSWER_COMPLETED";
            case TIMED_OUT -> "ANSWER_TIMED_OUT";
            case SKIPPED -> "ANSWER_SKIPPED";
            case RECORDING_FAILED -> "ANSWER_RECORDING_FAILED";
        };
        String type = switch (result) {
            case NEXT_PARTICIPANT -> eventPrefix + "_SPEAKER_CHANGED";
            case NEXT_QUESTION -> eventPrefix + "_QUESTION_CHANGED";
            case INTERVIEW_FINISHED -> eventPrefix + "_INTERVIEW_FINISHED";
        };
        broadcastProgress(type, roomId, actorUserId, session);
        if (result == GroupInterviewSessionEntity.AnswerCompletionResult.INTERVIEW_FINISHED) {
            triggerComparison(roomId);
        }
    }

    private GroupInterviewAnswerEntity createAnswer(
            GroupInterviewSessionEntity session,
            Long actorUserId,
            Instant startedAt
    ) {
        GroupQuestionEntity question = currentQuestion(session);
        return answerRepository.saveAndFlush(
                GroupInterviewAnswerEntity.builder()
                        .groupInterview(session.getInterview())
                        .participantUserId(actorUserId)
                        .questionId(question == null ? null : question.getId())
                        .round(session.getCurrentRound())
                        .questionIndex(session.getCurrentQuestionIndex())
                        .answerStartedAt(startedAt)
                        .build()
        );
    }

    private void scheduleAnswerStartDeadline(GroupInterviewSessionEntity session) {
        session.scheduleAnswerStartDeadline(
                Instant.now().plus(answerStartGraceSeconds, ChronoUnit.SECONDS)
        );
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
