package com.example.interviewfitbe.domain.followup.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "follow_up_answers")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FollowUpAnswerEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 어떤 면접에서 나온 꼬리질문 답변인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    // 어떤 꼬리질문에 대한 답변인지
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_question_id", nullable = false)
    private FollowUpQuestion followUpQuestion;

    // 꼬리질문 답변 텍스트
    @Column(columnDefinition = "TEXT")
    private String answerText;

    // 전체 면접 영상 기준 꼬리질문 답변 시작 시각
    private Double startTime;

    // 전체 면접 영상 기준 꼬리질문 답변 종료 시각
    private Double endTime;

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

    public void updateAnswerText(String answerText) {
        this.answerText = answerText;
    }

    public void updateTimestamp(Double startTime, Double endTime) {
        this.startTime = startTime;
        this.endTime = endTime;
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
}