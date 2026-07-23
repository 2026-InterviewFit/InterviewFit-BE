package com.example.interviewfitbe.domain.eye_blinking.entity;

import com.example.interviewfitbe.domain.eye_blinking.enums.EyeBlinkingGrade;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "eye_blinking_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EyeBlinkingReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * 전체 면접 기준 분석 결과.
     * 단독 테스트나 임시 분석을 위해 nullable 허용.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /*
     * 질문별 답변 구간에 대한 분석 결과일 경우 연결.
     * 전체 면접 영상 분석 결과라면 null 가능.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_answer_id")
    private InterviewAnswerEntity interviewAnswer;

    private Double durationSeconds;

    private Integer blinkCount;

    private Double blinkRate;

    private Integer notDetectedCount;

    private Double notDetectedRatio;

    private Double score;

    @Enumerated(EnumType.STRING)
    private EyeBlinkingGrade grade;

    @Column(columnDefinition = "TEXT")
    private String feedback;

    @Column(columnDefinition = "TEXT")
    private String resultJson;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public void assignGrade() {
        this.grade = EyeBlinkingGrade.fromScore(this.score);
    }
}