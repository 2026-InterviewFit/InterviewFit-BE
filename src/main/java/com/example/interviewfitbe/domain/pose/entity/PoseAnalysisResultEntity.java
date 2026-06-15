package com.example.interviewfitbe.domain.pose.entity;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.pose.enums.PoseGrade;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Builder
@AllArgsConstructor
@Entity
@Table(name = "pose_analysis_results")
public class PoseAnalysisResultEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 기존에는 OneToOne이었지만,
     * 질문별 영상 분석을 위해 한 interview에 여러 pose 결과가 필요하므로 ManyToOne으로 변경한다.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private InterviewEntity interview;

    /**
     * 질문별 답변 영상과 연결.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_answer_id")
    private InterviewAnswerEntity interviewAnswer;

    private Integer totalDuration;

    private Integer badCount;
    private Integer badDuration;

    private Integer shoulderBadCount;
    private Integer faceBadCount;

    private Integer shoulderBadDuration;
    private Integer faceBadDuration;

    private Double badRatio;

    private Integer score;

    @Enumerated(EnumType.STRING)
    private PoseGrade grade;
}