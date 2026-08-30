package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerAnalysisEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.speech_coaching.dto.ExternalAiAnalysis;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GroupInterviewAnswerAnalysisResponse {
    private Long analysisId;
    private Long answerId;
    private Long roomId;
    private Long participantUserId;
    private Integer round;
    private Integer questionIndex;
    private AnalysisStatus status;
    private String speechText;
    private String speechResultJson;
    private String aiEvaluationJson;
    private ExternalAiAnalysis externalAiAnalysis;
    private String eyeBlinkingResultJson;
    private String eyeGazeResultJson;
    private String poseResultJson;
    private String errorMessage;
    private Instant analysisStartedAt;
    private Instant analysisCompletedAt;

    public static GroupInterviewAnswerAnalysisResponse from(
            GroupInterviewAnswerAnalysisEntity analysis,
            ExternalAiAnalysis externalAiAnalysis
    ) {
        var answer = analysis.getAnswer();
        return GroupInterviewAnswerAnalysisResponse.builder()
                .analysisId(analysis.getId())
                .answerId(answer.getId())
                .roomId(answer.getGroupInterview().getId())
                .participantUserId(answer.getParticipantUserId())
                .round(answer.getRound())
                .questionIndex(answer.getQuestionIndex())
                .status(analysis.getStatus())
                .speechText(analysis.getSpeechText())
                .speechResultJson(analysis.getSpeechResultJson())
                .aiEvaluationJson(analysis.getAiEvaluationJson())
                .externalAiAnalysis(externalAiAnalysis)
                .eyeBlinkingResultJson(analysis.getEyeBlinkingResultJson())
                .eyeGazeResultJson(analysis.getEyeGazeResultJson())
                .poseResultJson(analysis.getPoseResultJson())
                .errorMessage(analysis.getErrorMessage())
                .analysisStartedAt(analysis.getAnalysisStartedAt())
                .analysisCompletedAt(analysis.getAnalysisCompletedAt())
                .build();
    }
}
