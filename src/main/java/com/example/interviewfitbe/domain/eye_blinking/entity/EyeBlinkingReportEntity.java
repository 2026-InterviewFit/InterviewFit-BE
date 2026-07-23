package com.example.interviewfitbe.domain.eye_blinking.entity;

import com.example.interviewfitbe.domain.eye_blinking.enums.EyeBlinkingGrade;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(name = "eye_blinking_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class EyeBlinkingReportEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /*
     * Interview 삭제 시 눈깜빡임 리포트도 같이 삭제.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /*
     * Answer 삭제 시 해당 답변 기준 눈깜빡임 리포트도 같이 삭제.
     * 전체 면접 영상 분석 결과라면 null 가능.
     */
    @OnDelete(action = OnDeleteAction.CASCADE)
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