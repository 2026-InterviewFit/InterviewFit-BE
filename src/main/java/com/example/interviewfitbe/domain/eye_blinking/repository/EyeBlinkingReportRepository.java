package com.example.interviewfitbe.domain.eye_blinking.repository;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReportEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EyeBlinkingReportRepository
        extends JpaRepository<EyeBlinkingReportEntity, Long> {

    /**
     * 면접 기준 가장 최근 눈 깜빡임 분석
     */
    Optional<EyeBlinkingReportEntity>
    findTopByInterview_IdOrderByCreatedAtDesc(
            Long interviewId
    );

    /**
     * 발표 전체 영상 기준 눈 깜빡임 분석
     *
     * interviewAnswer가 null인 데이터만 조회한다.
     */
    Optional<EyeBlinkingReportEntity>
    findTopByInterview_IdAndInterviewAnswerIsNullOrderByCreatedAtDesc(
            Long interviewId
    );

    /**
     * 질문 답변별 가장 최근 눈 깜빡임 분석
     */
    Optional<EyeBlinkingReportEntity>
    findTopByInterviewAnswer_IdOrderByCreatedAtDesc(
            Long interviewAnswerId
    );

    /**
     * 면접의 모든 눈 깜빡임 분석
     */
    List<EyeBlinkingReportEntity>
    findByInterview_IdOrderByCreatedAtAsc(
            Long interviewId
    );
}