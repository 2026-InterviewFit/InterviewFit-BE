package com.example.interviewfitbe.domain.pt_interview.dto;

import com.example.interviewfitbe.domain.speech_coaching.enums.SpeechScoreStatus;

/**
 * PT 발표 영상 음성 상세 분석
 */
public record PtSpeechDetailResponse(

        Double score,

        Double speechScore,

        SpeechScoreStatus speechScoreStatus,

        String speechScoreReason,

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
