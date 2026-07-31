package com.example.interviewfitbe.domain.interview.entity;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.pt_interview.entity.PtQuestionEntity;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "interview_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class InterviewAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Interview 삭제 시 해당 면접 답변도 같이 삭제.
     *
     * Question이 Interview와 Answer 사이를 관리하므로
     * interview_id 자체는 nullable 허용.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /*
     * 일반 면접 질문.
     *
     * PT 면접 답변에서는 사용하지 않으므로
     * nullable을 허용한다.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionEntity question;

    /*
     * PT 면접 질문.
     *
     * 일반 면접 답변에서는 null이며,
     * PT 면접 답변을 저장할 때만 연결한다.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pt_question_id")
    private PtQuestionEntity ptQuestion;

    @Column(nullable = false)
    private Integer answerOrder;

    /*
     * 전체 면접 영상 기준 답변 시작 시각.
     * 단위: 초
     */
    private Double startTime;

    /*
     * 전체 면접 영상 기준 답변 종료 시각.
     * 단위: 초
     */
    private Double endTime;

    /*
     * STT 결과 또는 사용자의 답변 텍스트.
     */
    @Column(columnDefinition = "TEXT")
    private String answerText;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus analysisStatus = AnalysisStatus.UPLOADED;

    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public void updateTimestamp(
            Double startTime,
            Double endTime
    ) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void updateAnswerText(
            String answerText
    ) {
        this.answerText = answerText;
    }

    public void markUploaded() {
        this.analysisStatus =
                AnalysisStatus.UPLOADED;

        this.errorMessage =
                null;
    }

    public void markRunning() {
        this.analysisStatus =
                AnalysisStatus.RUNNING;

        this.errorMessage =
                null;
    }

    public void markDone() {
        this.analysisStatus =
                AnalysisStatus.DONE;

        this.errorMessage =
                null;
    }

    public void markFailed(
            String errorMessage
    ) {
        this.analysisStatus =
                AnalysisStatus.FAILED;

        this.errorMessage =
                errorMessage;
    }

    /*
     * 일반 면접 또는 PT 면접에서
     * 연결된 Interview ID 반환.
     */
    public Long getInterviewIdSafely() {

        InterviewEntity interviewEntity =
                getInterviewSafely();

        return interviewEntity == null
                ? null
                : interviewEntity.getId();
    }

    /*
     * 일반 면접 또는 PT 면접에서
     * 연결된 Interview 반환.
     */
    public InterviewEntity getInterviewSafely() {

        if (this.interview != null) {
            return this.interview;
        }

        if (this.question != null) {
            return this.question
                    .getInterview();
        }

        if (
                this.ptQuestion != null
                        && this.ptQuestion
                        .getPtInterview() != null
        ) {

            return this.ptQuestion
                    .getPtInterview()
                    .getInterview();
        }

        return null;
    }

    /*
     * 기존 일반 질문 ID 조회용.
     */
    public Long getQuestionId() {

        return this.question == null
                ? null
                : this.question.getId();
    }

    /*
     * PT 질문 ID 조회용.
     */
    public Long getPtQuestionId() {

        return this.ptQuestion == null
                ? null
                : this.ptQuestion.getId();
    }

    /*
     * 일반 면접과 PT 면접 모두에서
     * 질문 내용을 반환한다.
     */
    public String getQuestionText() {

        if (this.question != null) {
            return this.question
                    .getQuestion();
        }

        if (this.ptQuestion != null) {
            return this.ptQuestion
                    .getQuestion();
        }

        return null;
    }

    /*
     * 현재 답변이 PT 면접 답변인지 확인.
     */
    public boolean isPtAnswer() {
        return this.ptQuestion != null;
    }

    /*
     * 기존 코드 호환용.
     * 최종 구조에서는 영상은 videos/interview에서 관리함.
     */
    public String getVideoObjectKey() {
        return null;
    }
}