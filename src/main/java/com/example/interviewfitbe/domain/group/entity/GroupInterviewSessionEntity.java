package com.example.interviewfitbe.domain.group.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "group_interview_sessions")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GroupInterviewSessionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    /**
     * 연결된 면접 방
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "group_interview_id")
    private GroupInterviewEntity interview;


    /**
     * 현재 ROUND
     * 1~3
     */
    private Integer currentRound;


    /**
     * 현재 질문 번호
     */
    private Integer currentQuestionIndex;


    /**
     * 현재 답변자 순서
     */
    private Integer currentParticipantIndex;


    @Enumerated(EnumType.STRING)
    private InterviewStatus status;



    public void start() {

        this.currentRound = 1;
        this.currentQuestionIndex = 1;
        this.currentParticipantIndex = 0;
        this.status = InterviewStatus.QUESTION_VIEW;

    }


    public void startAnswer() {

        this.status = InterviewStatus.ANSWERING;

    }


    public void nextParticipant(int totalParticipants) {


        if(currentParticipantIndex + 1 < totalParticipants){

            currentParticipantIndex++;

        } else {

            currentParticipantIndex = 0;
            currentQuestionIndex++;

        }


    }

    public void endAnswer(int participantCount) {

        // 답변 종료 후 참가자 전환
        this.status = InterviewStatus.TRANSITION;

        nextParticipant(participantCount);

        // 다음 참가자 답변 준비
        this.status = InterviewStatus.QUESTION_VIEW;

    }

    public void nextQuestion() {

        this.currentQuestionIndex++;

        this.currentParticipantIndex = 0;

        this.status = InterviewStatus.QUESTION_VIEW;

    }

    /**
     * 다음 라운드로 이동한다.
     * 라운드가 바뀔 때마다 첫 답변자를 한 명씩 순환시킨다.
     */
    public void moveToNextRound(int participantCount) {

        if (participantCount <= 0) {
            throw new IllegalArgumentException("라운드 전환을 위해 최소 1명의 참가자가 필요합니다.");
        }

        if (this.currentRound == null || this.currentQuestionIndex == null) {
            throw new IllegalStateException("아직 시작되지 않은 면접 세션입니다.");
        }

        if (this.currentRound >= 3) {
            this.status = InterviewStatus.FINISHED;
            return;
        }

        this.currentRound++;
        this.currentQuestionIndex++;
        this.currentParticipantIndex = (this.currentRound - 1) % participantCount;
        this.status = InterviewStatus.QUESTION_VIEW;
    }

    public void finish(){

        this.status = InterviewStatus.FINISHED;

    }


}
