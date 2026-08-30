package com.example.interviewfitbe.domain.group.entity;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;

@Entity
@Table(name = "group_interview_answer_analyses")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class GroupInterviewAnswerAnalysisEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "group_interview_answer_id", nullable = false, unique = true)
    private GroupInterviewAnswerEntity answer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AnalysisStatus status = AnalysisStatus.UPLOADED;

    @Column(name = "speech_text", columnDefinition = "TEXT")
    private String speechText;

    @Column(name = "speech_result_json", columnDefinition = "TEXT")
    private String speechResultJson;

    @Column(name = "ai_evaluation_json", columnDefinition = "TEXT")
    private String aiEvaluationJson;

    @Column(name = "external_ai_analysis_json", columnDefinition = "TEXT")
    private String externalAiAnalysisJson;

    @Column(name = "eye_blinking_result_json", columnDefinition = "TEXT")
    private String eyeBlinkingResultJson;

    @Column(name = "eye_gaze_result_json", columnDefinition = "TEXT")
    private String eyeGazeResultJson;

    @Column(name = "pose_result_json", columnDefinition = "TEXT")
    private String poseResultJson;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "analysis_started_at")
    private Instant analysisStartedAt;

    @Column(name = "analysis_completed_at")
    private Instant analysisCompletedAt;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    private Instant updatedAt;

    @Version
    private Long version;

    public void markUploaded() {
        this.status = AnalysisStatus.UPLOADED;
        this.errorMessage = null;
        this.analysisStartedAt = null;
        this.analysisCompletedAt = null;
    }

    public void markRunning(Instant startedAt) {
        this.status = AnalysisStatus.RUNNING;
        this.errorMessage = null;
        this.analysisStartedAt = startedAt;
        this.analysisCompletedAt = null;
    }

    public void markDone(
            String speechText,
            String speechResultJson,
            String aiEvaluationJson,
            String externalAiAnalysisJson,
            String eyeBlinkingResultJson,
            String eyeGazeResultJson,
            String poseResultJson,
            Instant completedAt
    ) {
        this.speechText = speechText;
        this.speechResultJson = speechResultJson;
        this.aiEvaluationJson = aiEvaluationJson;
        this.externalAiAnalysisJson = externalAiAnalysisJson;
        this.eyeBlinkingResultJson = eyeBlinkingResultJson;
        this.eyeGazeResultJson = eyeGazeResultJson;
        this.poseResultJson = poseResultJson;
        this.status = AnalysisStatus.DONE;
        this.errorMessage = null;
        this.analysisCompletedAt = completedAt;
    }

    public void markFailed(String errorMessage, Instant completedAt) {
        this.status = AnalysisStatus.FAILED;
        this.errorMessage = errorMessage;
        this.analysisCompletedAt = completedAt;
    }
}
