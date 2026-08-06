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


    public void finish(){

        this.status = InterviewStatus.FINISHED;

    }


}