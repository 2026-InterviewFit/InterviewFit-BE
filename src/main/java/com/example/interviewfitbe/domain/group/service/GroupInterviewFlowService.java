package com.example.interviewfitbe.domain.group.service;


import com.example.interviewfitbe.domain.group.entity.*;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewSessionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


@Service
@RequiredArgsConstructor
public class GroupInterviewFlowService {


    private final GroupInterviewRepository groupInterviewRepository;

    private final GroupInterviewSessionRepository sessionRepository;



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