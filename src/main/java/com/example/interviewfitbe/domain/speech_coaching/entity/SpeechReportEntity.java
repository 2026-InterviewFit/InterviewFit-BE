package com.example.interviewfitbe.domain.speech_coaching.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.speech_coaching.enums.SpeechScoreStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "speech_report")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SpeechReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 어떤 면접의 음성 분석인지
     *
     * 한 면접 안에
     * - 전체 발표 SpeechReport
     * - 질문별 SpeechReport
     * 가 여러 개 존재할 수 있으므로 ManyToOne 사용
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewEntity interview;

    /**
     * 질문 답변 단위 SpeechReport
     *
     * PT 전체 발표 분석인 경우 null
     * 일반/그룹/PT 질문 답변 분석인 경우 answerId 저장
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_answer_id")
    private InterviewAnswerEntity interviewAnswer;

    /**
     * 음성 종합 점수
     */
    private Double score;

    /**
     * 음성 점수 상태
     */
    @Enumerated(EnumType.STRING)
    private SpeechScoreStatus speechScoreStatus;

    /**
     * 점수를 계산하지 못한 경우 사유
     */
    @Column(columnDefinition = "TEXT")
    private String speechScoreReason;

    /**
     * 전체 STT 결과
     */
    @Column(columnDefinition = "TEXT")
    private String textScript;

    /**
     * 필러 분석
     */
    private Integer totalFillerCount;

    @Column(columnDefinition = "TEXT")
    private String fillerDetailsJson;

    /**
     * 반복 표현 분석
     */
    @Column(columnDefinition = "TEXT")
    private String repeatedExpressionsJson;

    /**
     * 말하기 속도 분석
     */
    private String paceEvaluation;

    @Column(columnDefinition = "TEXT")
    private String paceFeedback;

    private Double charactersPerSecond;

    /**
     * 침묵 분석
     */
    private Double totalSilenceSeconds;

    private Double pauseRatePercent;

    private Integer criticalSilenceCount;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
}