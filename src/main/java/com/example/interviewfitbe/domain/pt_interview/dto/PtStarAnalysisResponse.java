package com.example.interviewfitbe.domain.pt_interview.dto;

/**
 * PT 질문 답변 STAR 구조 평가
 *
 * 각 항목은 0~10점
 * totalScore는 최대 40점
 */
public record PtStarAnalysisResponse(

        Integer situation,

        Integer task,

        Integer action,

        Integer result,

        Integer totalScore

) {
}
