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
     * 실제 면접 세션.
     *
     * 전체 면접 영상처럼 특정 질문이 없는 답변도 있으므로
     * interview_id는 그대로 유지한다.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /*
     * 이번 면접에서 사용된 질문 연결 정보.
     *
     * QuestionEntity는 여러 면접에서 재사용되는 원본이므로
     * 일반 면접 답변은 QuestionEntity 자체가 아니라
     * InterviewQuestionEntity를 통해 이번 면접의 질문과 연결한다.
     *
     * 기존 데이터와 PT/전체영상 흐름 호환을 위해 nullable 허용.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_question_id")
    private InterviewQuestionEntity interviewQuestion;

    /*
     * 기존 코드 호환용 일반 질문 원본 참조.
     *
     * 신규 일반 면접 답변에서는 interviewQuestion을 기준 관계로 사용하고,
     * 이 필드는 기존 조회/분석 코드가 바로 깨지지 않도록 함께 저장한다.
     * 최종 정리 단계에서 필요 여부를 다시 판단한다.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id")
    private QuestionEntity question;

    /*
     * PT 면접 질문.
     * 일반 면접 답변에서는 null.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pt_question_id")
    private PtQuestionEntity ptQuestion;

    @Column(nullable = false)
    private Integer answerOrder;

    private Double startTime;

    private Double endTime;

    @Column(columnDefinition = "TEXT")
    private String answerText;

    /**
     * 질문별/전체 답변 원본의 MinIO object key.
     *
     * 2단계에서는 질문별 답변을 먼저 저장만 하고,
     * 3단계에서 이 key를 이용해 비동기 Speech/Gemini 분석을 시작한다.
     */
    @Column(name = "video_object_key", length = 500)
    private String videoObjectKey;

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
        this.analysisStatus = AnalysisStatus.UPLOADED;
        this.errorMessage = null;
    }

    public void markRunning() {
        this.analysisStatus = AnalysisStatus.RUNNING;
        this.errorMessage = null;
    }

    public void markDone() {
        this.analysisStatus = AnalysisStatus.DONE;
        this.errorMessage = null;
    }

    public void markFailed(
            String errorMessage
    ) {
        this.analysisStatus = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public Long getInterviewIdSafely() {
        InterviewEntity interviewEntity = getInterviewSafely();

        return interviewEntity == null
                ? null
                : interviewEntity.getId();
    }

    public InterviewEntity getInterviewSafely() {

        if (this.interview != null) {
            return this.interview;
        }

        if (
                this.interviewQuestion != null
                        && this.interviewQuestion.getInterview() != null
        ) {
            return this.interviewQuestion.getInterview();
        }

        /*
         * 1단계 이전 데이터 호환용 fallback.
         * Question.interview 제거 전까지만 유지한다.
         */
        if (this.question != null) {
            return this.question.getInterview();
        }

        if (
                this.ptQuestion != null
                        && this.ptQuestion.getPtInterview() != null
        ) {
            return this.ptQuestion
                    .getPtInterview()
                    .getInterview();
        }

        return null;
    }

    /**
     * 이번 면접에서 사용된 질문 연결 ID.
     */
    public Long getInterviewQuestionId() {
        return this.interviewQuestion == null
                ? null
                : this.interviewQuestion.getId();
    }

    /**
     * 일반 질문 원본 ID.
     * 신규 데이터에서는 InterviewQuestion -> Question 경로를 우선 사용한다.
     */
    public Long getQuestionId() {

        if (
                this.interviewQuestion != null
                        && this.interviewQuestion.getQuestion() != null
        ) {
            return this.interviewQuestion
                    .getQuestion()
                    .getId();
        }

        return this.question == null
                ? null
                : this.question.getId();
    }

    public Long getPtQuestionId() {
        return this.ptQuestion == null
                ? null
                : this.ptQuestion.getId();
    }

    public String getQuestionText() {

        if (
                this.interviewQuestion != null
                        && this.interviewQuestion.getQuestion() != null
        ) {
            return this.interviewQuestion
                    .getQuestion()
                    .getQuestion();
        }

        if (this.question != null) {
            return this.question.getQuestion();
        }

        if (this.ptQuestion != null) {
            return this.ptQuestion.getQuestion();
        }

        return null;
    }

    public boolean isPtAnswer() {
        return this.ptQuestion != null;
    }

    public String getVideoObjectKey() {
        return this.videoObjectKey;
    }
}
