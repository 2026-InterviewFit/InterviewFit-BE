package com.example.interviewfitbe.domain.pt_interview.dto;

/**
 * PT 발표 영상 음성 상세 분석
 */
public record PtSpeechDetailResponse(

        Double score,

        String textScript,

        Integer totalFillerCount,

        String fillerDetailsJson,

        String repeatedExpressionsJson,

        String paceEvaluation,

        String paceFeedback,

        Double charactersPerSecond,

        Double totalSilenceSeconds,

        Double pauseRatePercent,

        Integer criticalSilenceCount

) {
}
