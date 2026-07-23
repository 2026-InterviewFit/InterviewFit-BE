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
    private String analysisStatus;
    private boolean completed;

    /**
     * eye/pose는 interview 단위 리포트.
     * gaze/speech는 아직 답변별. 팀원 리팩토링 이후 최상위로 이동 예정.
     */
    private EyeReport eye;
    private PoseReport pose;

    private List<AnswerReport> answers;

    @Getter
    @Builder
    @AllArgsConstructor
    public static class AnswerReport {

        private Long answerId;
        private Long questionId;
        private String questionText;
        private String answerText;
        private Integer answerOrder;
        private Double startTime;
        private Double endTime;
        private AnalysisStatus analysisStatus;
        private String errorMessage;

        // 팀원 리팩토링 대기중
        private GazeReport gaze;
        private SpeechReport speech;
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