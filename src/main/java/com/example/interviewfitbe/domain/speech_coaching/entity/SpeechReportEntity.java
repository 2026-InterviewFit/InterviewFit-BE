package com.example.interviewfitbe.domain.speech_coaching.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
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
     * 기존에는 OneToOne이었지만,
     * 질문별 답변마다 speech 결과가 생길 수 있으므로 ManyToOne으로 변경한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id", nullable = false)
    private InterviewEntity interview;

    /**
     * 질문별 답변 영상과 연결.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_answer_id")
    private InterviewAnswerEntity interviewAnswer;

    @Column(columnDefinition = "TEXT")
    private String textScript;

    private Integer totalFillerCount;

    @Column(columnDefinition = "TEXT")
    private String fillerDetailsJson;

    private String paceEvaluation;

    @Column(columnDefinition = "TEXT")
    private String paceFeedback;

    private Double charactersPerSecond;

    private Double totalSilenceSeconds;

    private Integer criticalSilenceCount;

    @Column(columnDefinition = "TEXT")
    private String aiEvaluationJson;

    @Column(columnDefinition = "TEXT")
    private String modelAnswer;

    @CreationTimestamp
    private LocalDateTime createdAt;
}