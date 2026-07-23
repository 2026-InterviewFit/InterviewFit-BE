package com.example.interviewfitbe.domain.interview.entity;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
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
     * Question 삭제 시 해당 질문의 답변도 같이 삭제.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

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

    public void updateTimestamp(Double startTime, Double endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
    }

    public void updateAnswerText(String answerText) {
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

    public void markFailed(String errorMessage) {
        this.analysisStatus = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    public Long getInterviewIdSafely() {
        if (this.interview != null) {
            return this.interview.getId();
        }

        if (this.question != null && this.question.getInterview() != null) {
            return this.question.getInterview().getId();
        }

        return null;
    }

    public InterviewEntity getInterviewSafely() {
        if (this.interview != null) {
            return this.interview;
        }

        if (this.question != null) {
            return this.question.getInterview();
        }

        return null;
    }

    /*
     * 기존 코드 호환용.
     */
    public Long getQuestionId() {
        return this.question == null ? null : this.question.getId();
    }

    /*
     * 기존 코드 호환용.
     */
    public String getQuestionText() {
        return this.question == null ? null : this.question.getQuestion();
    }

    /*
     * 기존 코드 호환용.
     * 최종 구조에서는 영상은 videos/interview에서 관리함.
     */
    public String getVideoObjectKey() {
        return null;
    }
}