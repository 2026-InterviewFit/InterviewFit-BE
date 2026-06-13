package com.example.interviewfitbe.domain.speech_coaching.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "speech_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA를 위한 필수 생성자
@AllArgsConstructor
@Builder
public class SpeechReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 면접 세션과 1:1 관계 매핑 (어떤 면접의 결과인지 보장)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewEntity interview;

    // 1. 대본 데이터
    @Column(columnDefinition = "TEXT")
    private String textScript;

    // 2. 간투어 (습관어) 분석
    private Integer totalFillerCount;

    @Column(columnDefinition = "TEXT")
    private String fillerDetailsJson;     // 예: {"음":3, "어":2} 형태의 JSON 문자열

    // 3. 발화 속도 분석
    private String paceEvaluation;        // 예: 적절함, 빠름 등

    @Column(columnDefinition = "TEXT")
    private String paceFeedback;          // 상세 피드백

    private Double charactersPerSecond;   // 초당 글자 수

    // 4. 침묵 분석
    private Double totalSilenceSeconds;   // 총 침묵 시간
    private Integer criticalSilenceCount; // 너무 긴 침묵 횟수

    // 5. AI 종합 평가
    @Column(columnDefinition = "TEXT")
    private String aiEvaluationJson;      // Gemini AI 결과 저장

    // [추가] 모범 답안 저장 필드
    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    @CreationTimestamp
    private LocalDateTime createdAt;      // 리포트 생성 시간
}