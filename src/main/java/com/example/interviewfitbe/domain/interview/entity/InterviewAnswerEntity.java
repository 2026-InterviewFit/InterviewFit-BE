package com.example.interviewfitbe.domain.interview.entity;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
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
     * Question이 Interview와 Answer 사이를 관리하므로
     * interview_id는 nullable 허용.
     *
     * answer -> question -> interview 로 면접을 찾을 수 있음.
     * 다만 기존 코드 호환과 조회 편의를 위해 컬럼 자체는 남겨둠.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /*
     * 어떤 질문에 대한 답변인지 연결.
     * 답변은 반드시 질문에 종속되므로 nullable=false.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "question_id", nullable = false)
    private QuestionEntity question;

    /*
     * 몇 번째 답변인지.
     * 예: Q1 = 1, Q2 = 2, Q3 = 3
     */
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

    /*
     * 분석 상태.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AnalysisStatus analysisStatus;

    /*
     * 분석 실패 시 에러 메시지.
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

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
}