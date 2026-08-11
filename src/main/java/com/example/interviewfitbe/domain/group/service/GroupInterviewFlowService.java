package com.example.interviewfitbe.domain.group.service;


import com.example.interviewfitbe.domain.group.entity.*;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewSessionRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class GroupInterviewFlowService {


    private final GroupInterviewRepository groupInterviewRepository;

    private final GroupInterviewSessionRepository sessionRepository;

    private final GroupParticipantRepository groupParticipantRepository;



    /**
     * 면접 시작
     */
    @Transactional
    public GroupInterviewSessionEntity start(Long roomId){


        GroupInterviewEntity interview =
                groupInterviewRepository.findById(roomId)
                        .orElseThrow();


        GroupInterviewSessionEntity session =
                GroupInterviewSessionEntity.builder()
                        .interview(interview)
                        .build();


        session.start();


        return sessionRepository.save(session);

    }



    /**
     * 답변 시작
     */
    @Transactional
    public void startAnswer(Long roomId){


        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        session.startAnswer();

    }



    /**
     * 다음 참가자
     */
    @Transactional
    public void nextParticipant(
            Long roomId,
            int participantCount
    ){

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        session.nextParticipant(participantCount);

    }

    /**
     * 답변 종료
     */
    @Transactional
    public void endAnswer(
            Long roomId,
            int participantCount
    ){

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        session.endAnswer(participantCount);

    }

    /**
     * 다음 질문 이동
     */
    @Transactional
    public GroupInterviewSessionEntity nextQuestion(Long roomId){

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        session.nextQuestion();

        return session;

    }

    /**
     * 다음 라운드 이동
     */
    @Transactional
    public GroupInterviewSessionEntity nextRound(Long roomId) {

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow(() -> new IllegalArgumentException(
                                "면접 세션을 찾을 수 없습니다. roomId=" + roomId
                        ));

        int participantCount = Math.toIntExact(
                groupParticipantRepository.countByRoomId(roomId)
        );

        session.moveToNextRound(participantCount);

        return session;
    }

    /**
     * 면접 종료
     */
    @Transactional
    public void finish(Long roomId){

        GroupInterviewSessionEntity session =
                sessionRepository.findByInterviewId(roomId)
                        .orElseThrow();


        session.finish();

    }

}
