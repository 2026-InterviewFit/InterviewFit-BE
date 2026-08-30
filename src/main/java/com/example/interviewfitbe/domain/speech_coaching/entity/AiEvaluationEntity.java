package com.example.interviewfitbe.domain.speech_coaching.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_evaluation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AiEvaluationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 질문별 답변과 1:1 연결
     */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_answer_id", nullable = false, unique = true)
    private InterviewAnswerEntity interviewAnswer;

    /**
     * STAR 구조 평가
     */
    @Column(columnDefinition = "TEXT")
    private String starEvaluationJson;

    /**
     * Gemini 피드백
     */
    @Column(columnDefinition = "TEXT")
    private String geminiFeedback;

    /**
     * 핵심 키워드
     */
    @Column(columnDefinition = "TEXT")
    private String keywordsJson;

    /**
     * AI 모범답안
     */
    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    /**
     * 외부 학습 AI 분석 전체 JSON
     */
    @Column(name = "external_ai_analysis_json", columnDefinition = "TEXT")
    private String externalAiAnalysisJson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void updateExternalAiAnalysisJson(String externalAiAnalysisJson) {
        this.externalAiAnalysisJson = externalAiAnalysisJson;
    }
}
