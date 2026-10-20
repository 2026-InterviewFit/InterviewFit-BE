package com.example.interviewfitbe.domain.interview.dto;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.speech_coaching.dto.ExternalAiAnalysis;
import com.example.interviewfitbe.domain.speech_coaching.enums.SpeechScoreStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class InterviewReportResponse {

    private Long interviewId;
    private Long userId;
    private Long jobRoleId;
    private String jobCategory;
    private String jobName;
    private LocalDateTime createdAt;
    private String analysisStatus;
    private boolean completed;

    /**
     * 전체 면접 영상 기준 비언어 분석 결과.
     */
    private EyeReport eye;
    private PoseReport pose;
    private GazeReport gaze;

    /**
     * 질문별 결과.
     *
     * 신규 면접에서는 각 기본 질문의 InterviewAnswer를 반환하고,
     * 기존 데이터에 질문별 답변이 없는 경우에는
     * 전체 면접 Answer를 fallback으로 반환한다.
     */
    private List<AnswerReport> answers;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AnswerReport {

        private Long answerId;
        private Long interviewQuestionId;
        private Long questionId;
        private String questionText;
        private String videoObjectKey;
        private String answerText;
        private Integer answerOrder;
        private Double startTime;
        private Double endTime;
        private AnalysisStatus analysisStatus;
        private String errorMessage;

        /**
         * 질문별 Gemini 내용 평가.
         */
        private AiEvaluationReport aiEvaluation;

        /**
         * 이 기본 답변을 source_answer_id로 사용하는
         * 꼬리질문 목록.
         */
        private List<FollowUpReport> followUps;

        /**
         * 기존 FE 호환용.
         *
         * gaze / speech의 정량 지표는
         * 전체 면접 영상 결과를 공유한다.
         */
        private GazeReport gaze;
        private SpeechReport speech;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FollowUpReport {

        private Long followUpQuestionId;
        private String questionText;
        private LocalDateTime createdAt;
        private List<FollowUpAnswerReport> answers;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class FollowUpAnswerReport {

        private Long followUpAnswerId;
        private String answerText;
        private Double startTime;
        private Double endTime;
        private AnalysisStatus analysisStatus;
        private String errorMessage;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AiEvaluationReport {

        private Long evaluationId;
        private String starEvaluationJson;
        private String geminiFeedback;
        private String keywordsJson;
        private String modelAnswer;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class EyeReport {

        private Long reportId;
        private Double durationSeconds;
        private Integer blinkCount;
        private Double blinkRate;
        private Integer notDetectedCount;
        private Double notDetectedRatio;
        private Double score;
        private String grade;
        private String feedback;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GazeReport {

        private Long reportId;
        private Double centerRatio;
        private Double gazeAwayRatio;
        private Integer gazeAwayCount;
        private Double score;
        private String evaluation;
        private String resultJson;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class PoseReport {

        private Long reportId;
        private Double totalDuration;
        private Integer badCount;
        private Double badDuration;
        private Integer shoulderBadCount;
        private Integer faceBadCount;
        private Double shoulderBadDuration;
        private Double faceBadDuration;
        private Double badRatio;
        private Integer score;
        private String grade;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SpeechReport {

        private Long reportId;

        private Double speechScore;
        private SpeechScoreStatus speechScoreStatus;
        private String speechScoreReason;

        private String textScript;

        private Integer totalFillerCount;
        private String fillerDetailsJson;
        private String repeatedExpressionsJson;

        private String paceEvaluation;
        private String paceFeedback;

        private Double charactersPerSecond;

        private Double totalSilenceSeconds;
        private Double pauseRatePercent;
        private Integer criticalSilenceCount;

        private Double volumeDbMean;
        private Double volumeDbStd;

        private String aiEvaluationJson;
        private String modelAnswer;

        private ExternalAiAnalysis externalAiAnalysis;
    }
}