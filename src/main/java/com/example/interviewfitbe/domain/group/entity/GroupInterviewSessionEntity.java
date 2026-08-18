package com.example.interviewfitbe.domain.group.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

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

    private Instant answerStartedAt;

    private Instant answerDeadlineAt;

    private Long currentAnswerId;

    @Version
    private Long version;



    public void start() {

        this.currentRound = 1;
        this.currentQuestionIndex = 1;
        this.currentParticipantIndex = 0;
        this.status = InterviewStatus.QUESTION_VIEW;

    }


    public void startAnswer(Instant startedAt, Instant deadlineAt) {
        if (this.status != InterviewStatus.QUESTION_VIEW) {
            throw new IllegalStateException("답변을 시작할 수 있는 상태가 아닙니다.");
        }
        this.status = InterviewStatus.ANSWERING;
        this.answerStartedAt = startedAt;
        this.answerDeadlineAt = deadlineAt;
    }

    public void assignCurrentAnswer(Long answerId) {
        if (this.status != InterviewStatus.ANSWERING) {
            throw new IllegalStateException("답변 중인 상태에서만 answerId를 지정할 수 있습니다.");
        }
        this.currentAnswerId = answerId;
    }


    public void finish(){

        this.status = InterviewStatus.FINISHED;

        clearAnswerTimer();

    }

    /**
     * 현재 질문의 답변을 완료하고 서버가 다음 상태를 결정한다.
     * 현재 질문은 라운드당 하나이며 참가자는 id 오름차순으로 답변한다.
     */
    public AnswerCompletionResult completeCurrentAnswer(int participantCount) {
        if (participantCount <= 0) {
            throw new IllegalArgumentException("답변 완료를 위해 최소 1명의 참가자가 필요합니다.");
        }
        if (this.status != InterviewStatus.ANSWERING) {
            throw new IllegalStateException("현재 답변 중인 상태가 아닙니다.");
        }

        clearAnswerTimer();
        if (this.currentParticipantIndex + 1 < participantCount) {
            this.currentParticipantIndex++;
            this.status = InterviewStatus.QUESTION_VIEW;
            return AnswerCompletionResult.NEXT_PARTICIPANT;
        }

        if (this.currentRound < 3) {
            this.currentRound++;
            this.currentQuestionIndex++;
            this.currentParticipantIndex = 0;
            this.status = InterviewStatus.QUESTION_VIEW;
            return AnswerCompletionResult.NEXT_QUESTION;
        }

        this.status = InterviewStatus.FINISHED;
        return AnswerCompletionResult.INTERVIEW_FINISHED;
    }

    private void clearAnswerTimer() {
        this.answerStartedAt = null;
        this.answerDeadlineAt = null;
        this.currentAnswerId = null;
    }

    public enum AnswerCompletionResult {
        NEXT_PARTICIPANT,
        NEXT_QUESTION,
        INTERVIEW_FINISHED
    }


}
