package com.example.interviewfitbe.domain.pt_interview.dto;

/**
 * PT 발표 영상 상세 분석
 */
public record PtPresentationAnalysisResponse(

        PtEyeBlinkingDetailResponse eyeBlinking,

        PtEyeGazeDetailResponse eyeGaze,

        PtSpeechDetailResponse speech

) {
}
