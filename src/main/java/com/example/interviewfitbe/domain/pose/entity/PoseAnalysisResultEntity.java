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
     * 하나의 면접에는 여러 질문 답변이 있고,
     * 질문별로 자세 분석 결과가 여러 개 생길 수 있다.
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