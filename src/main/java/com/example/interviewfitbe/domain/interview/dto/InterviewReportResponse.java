package com.example.interviewfitbe.domain.interview.dto;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
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

    /**
     * 전체 분석 상태.
     *
     * EMPTY / UPLOADED / RUNNING / DONE / FAILED
     */
    private String analysisStatus;

    /**
     * 모든 답변 분석이 DONE이면 true.
     */
    private boolean completed;

    private List<AnswerReport> answers;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AnswerReport {

        private Long answerId;

        private Long questionId;

        private String questionText;

        private String videoObjectKey;

        private Integer answerOrder;

        private AnalysisStatus analysisStatus;

        private String errorMessage;

        private EyeReport eye;

        /**
         * 시선 추적 분석 결과.
         */
        private GazeReport gaze;

        private PoseReport pose;

        private SpeechReport speech;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class EyeReport {

        private Long reportId;

        private Integer blinkCount;

        private Double blinkRate;

        private Double durationSeconds;

        private Double score;

        private String feedback;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class GazeReport {

        private Long reportId;

        /**
         * 정면 응시 비율.
         */
        private Double centerRatio;

        /**
         * 시선 이탈 비율.
         */
        private Double gazeAwayRatio;

        /**
         * 시선 이탈 횟수.
         */
        private Integer gazeAwayCount;

        /**
         * 시선 분석 점수.
         */
        private Double score;

        /**
         * 시선 분석 평가 문구.
         */
        private String evaluation;

        /**
         * 프레임별 상세 분석 JSON.
         */
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
    }
}