package com.example.interviewfitbe.domain.group.service;


import com.example.interviewfitbe.domain.group.dto.InterviewProgressResponse;
import com.example.interviewfitbe.domain.group.dto.GroupRoomEvent;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewSessionResponse;
import com.example.interviewfitbe.domain.group.entity.*;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewSessionRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import com.example.interviewfitbe.domain.group.repository.GroupQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class GroupInterviewFlowService {


    private final GroupInterviewRepository groupInterviewRepository;

    private final GroupInterviewSessionRepository sessionRepository;

    private final GroupParticipantRepository groupParticipantRepository;

    private final GroupQuestionRepository groupQuestionRepository;

    private final SimpMessagingTemplate messagingTemplate;



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

        validateHost(roomId, actorUserId);


        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        session.startAnswer();

        broadcastProgress("ANSWER_STARTED", roomId, actorUserId, session);

    }



    /**
     * 다음 참가자
     */
    @Transactional
    public void nextParticipant(
            Long roomId,
            Long actorUserId
    ){

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        validateHost(roomId, actorUserId);
        session.nextParticipant(participantCount(roomId));

        broadcastProgress("SPEAKER_CHANGED", roomId, actorUserId, session);

    }

    /**
     * 답변 종료
     */
    @Transactional
    public void endAnswer(
            Long roomId,
            Long actorUserId
    ){

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        validateHost(roomId, actorUserId);
        session.endAnswer(participantCount(roomId));

        broadcastProgress("ANSWER_ENDED", roomId, actorUserId, session);

    }

    /**
     * 다음 질문 이동
     */
    @Transactional
    public GroupInterviewSessionEntity nextQuestion(Long roomId, Long actorUserId){

        validateHost(roomId, actorUserId);

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        session.nextQuestion();

        broadcastProgress("QUESTION_CHANGED", roomId, actorUserId, session);

        return session;

    }

    /**
     * 다음 라운드 이동
     */
    @Transactional
    public GroupInterviewSessionEntity nextRound(Long roomId, Long actorUserId) {

        validateHost(roomId, actorUserId);

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "면접 세션을 찾을 수 없습니다. roomId=" + roomId
                        ));

        int participantCount = Math.toIntExact(
                groupParticipantRepository.countByRoomId(roomId)
        );

        session.moveToNextRound(participantCount);

        broadcastProgress("ROUND_CHANGED", roomId, actorUserId, session);

        return session;
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

        broadcastProgress("INTERVIEW_FINISHED", roomId, actorUserId, session);

    }

    private void broadcastProgress(
            String type,
            Long roomId,
            Long actorUserId,
            GroupInterviewSessionEntity session
    ) {
        messagingTemplate.convertAndSend(
                "/topic/room/" + roomId,
                GroupRoomEvent.progress(
                        type,
                        roomId,
                        actorUserId,
                        toProgress(roomId, session)
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

    private void validateAllReady(Long roomId) {
        var participants = groupParticipantRepository.findAllByRoomIdOrderByIdAsc(roomId);
        if (participants.size() < 2
                || participants.stream().anyMatch(p -> !"READY".equals(p.getStatus()))) {
            throw new IllegalStateException("최소 2명의 참가자가 모두 준비 완료 상태여야 합니다.");
        }
    }

    private void validateHost(Long roomId, Long userId) {
        GroupInterviewEntity interview = groupInterviewRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("면접 방을 찾을 수 없습니다."));
        validateHost(interview, userId);
    }

    private void validateHost(GroupInterviewEntity interview, Long userId) {
        if (!interview.getHostUserId().equals(userId)) {
            throw new AccessDeniedException("방장만 면접 진행 상태를 변경할 수 있습니다.");
        }
    }

}
