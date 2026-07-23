package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReport;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import com.example.interviewfitbe.domain.eye_gaze.entity.EyeGazeReport;
import com.example.interviewfitbe.domain.eye_gaze.repository.EyeGazeReportRepository;
import com.example.interviewfitbe.domain.interview.dto.InterviewReportResponse;
import com.example.interviewfitbe.domain.interview.dto.InterviewStartRequest;
import com.example.interviewfitbe.domain.interview.dto.InterviewStartResponse;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import com.example.interviewfitbe.domain.job.exception.JobRoleErrorCode;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import com.example.interviewfitbe.domain.pose.entity.PoseReportEntity;
import com.example.interviewfitbe.domain.pose.repository.PoseReportRepository;
import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import com.example.interviewfitbe.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@RequiredArgsConstructor
@Service
public class InterviewService {

    private final UserRepository userRepository;
    private final JobRoleRepository jobRoleRepository;
    private final InterviewRepository interviewRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;

    private final EyeBlinkingReportRepository eyeBlinkingReportRepository;
    private final EyeGazeReportRepository eyeGazeReportRepository;
    private final PoseReportRepository poseReportRepository;
    private final SpeechReportRepository speechReportRepository;

    @Transactional
    public InterviewStartResponse startInterview(InterviewStartRequest request) {
        if (request.getJobRoleId() == null) {
            throw new IllegalArgumentException("jobRoleId는 필수입니다.");
        }

        UserEntity user = findOrCreateUser(request.getUserId());

        JobRoleEntity jobRole = jobRoleRepository
                .findById(request.getJobRoleId())
                .orElseThrow(() -> new BaseException(JobRoleErrorCode.JOB_ROLE_NOT_FOUND));

        InterviewEntity interview = InterviewEntity.builder()
                .user(user)
                .jobRole(jobRole)
                .build();

        InterviewEntity savedInterview = interviewRepository.save(interview);

        return InterviewStartResponse.builder()
                .interviewId(savedInterview.getId())
                .userId(user.getId())
                .jobRoleId(jobRole.getId())
                .jobName(jobRole.getName())
                .status("STARTED")
                .build();
    }

    @Transactional(readOnly = true)
    public InterviewReportResponse getInterviewReport(Long interviewId) {
        InterviewEntity interview = interviewRepository
                .findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 면접을 찾을 수 없습니다. interviewId=" + interviewId
                ));

        List<InterviewAnswerEntity> answers =
                interviewAnswerRepository.findByInterview_IdOrderByAnswerOrderAsc(interviewId);

        List<InterviewReportResponse.AnswerReport> answerReports =
                answers.stream()
                        .map(this::toAnswerReport)
                        .toList();

        String overallStatus = determineOverallStatus(answers);

        boolean completed =
                !answers.isEmpty()
                        && answers.stream()
                        .allMatch(answer -> answer.getAnalysisStatus() == AnalysisStatus.DONE);

        JobRoleEntity jobRole = interview.getJobRole();

        return InterviewReportResponse.builder()
                .interviewId(interview.getId())
                .userId(interview.getUser().getId())
                .jobRoleId(jobRole.getId())
                .jobCategory(jobRole.getCategory())
                .jobName(jobRole.getName())
                .createdAt(interview.getCreatedAt())
                .analysisStatus(overallStatus)
                .completed(completed)
                .answers(answerReports)
                .build();
    }

    private InterviewReportResponse.AnswerReport toAnswerReport(
            InterviewAnswerEntity answer
    ) {
        return InterviewReportResponse.AnswerReport.builder()
                .answerId(answer.getId())
                .questionId(answer.getQuestionId())
                .questionText(answer.getQuestionText())
                .videoObjectKey(answer.getVideoObjectKey())
                .answerOrder(answer.getAnswerOrder())
                .analysisStatus(answer.getAnalysisStatus())
                .errorMessage(answer.getErrorMessage())
                .eye(toEyeReport(answer))
                .gaze(toGazeReport(answer))
                .pose(toPoseReport(answer))
                .speech(toSpeechReport(answer))
                .build();
    }

    private InterviewReportResponse.EyeReport toEyeReport(
            InterviewAnswerEntity answer
    ) {
        return eyeBlinkingReportRepository
                .findTopByInterviewAnswer_IdOrderByCreatedAtDesc(answer.getId())
                .map(this::toEyeReport)
                .orElseGet(() -> {
                    Long interviewId = answer.getInterviewIdSafely();

                    if (interviewId == null) {
                        return null;
                    }

                    return eyeBlinkingReportRepository
                            .findTopByInterview_IdOrderByCreatedAtDesc(interviewId)
                            .map(this::toEyeReport)
                            .orElse(null);
                });
    }

    private InterviewReportResponse.EyeReport toEyeReport(
            EyeBlinkingReport report
    ) {
        return InterviewReportResponse.EyeReport.builder()
                .reportId(report.getId())
                .blinkCount(report.getBlinkCount())
                .blinkRate(report.getBlinkRate())
                .durationSeconds(report.getDurationSeconds())
                .score(report.getScore())
                .feedback(report.getFeedback())
                .build();
    }

    private InterviewReportResponse.GazeReport toGazeReport(
            InterviewAnswerEntity answer
    ) {
        return eyeGazeReportRepository
                .findTopByInterviewIdOrderByCreatedAtDesc(
                        answer.getInterview().getId()
                )
                .map(this::toGazeReport)
                .orElse(null);
    }

    private InterviewReportResponse.GazeReport toGazeReport(
            EyeGazeReport report
    ) {
        return InterviewReportResponse.GazeReport.builder()
                .reportId(report.getId())
                .centerRatio(report.getCenterRatio())
                .gazeAwayRatio(report.getGazeAwayRatio())
                .gazeAwayCount(report.getGazeAwayCount())
                .score(report.getScore())
                .evaluation(report.getEvaluation())
                .resultJson(report.getResultJson())
                .build();
    }

    private InterviewReportResponse.PoseReport toPoseReport(
            InterviewAnswerEntity answer
    ) {
        Long interviewId = answer.getInterviewIdSafely();

        if (interviewId == null) {
            return null;
        }

        return poseReportRepository
                .findTopByInterview_IdOrderByIdDesc(interviewId)
                .map(this::toPoseReport)
                .orElse(null);
    }

    private InterviewReportResponse.PoseReport toPoseReport(
            PoseReportEntity report
    ) {
        return InterviewReportResponse.PoseReport.builder()
                .reportId(report.getId())
                .totalDuration(report.getTotalDuration())
                .badCount(report.getBadCount())
                .badDuration(report.getBadDuration())
                .shoulderBadCount(report.getShoulderBadCount())
                .faceBadCount(report.getFaceBadCount())
                .shoulderBadDuration(report.getShoulderBadDuration())
                .faceBadDuration(report.getFaceBadDuration())
                .badRatio(report.getBadRatio())
                .score(report.getScore())
                .grade(report.getGrade() == null ? null : report.getGrade().name())
                .build();
    }

    private InterviewReportResponse.SpeechReport toSpeechReport(
            InterviewAnswerEntity answer
    ) {
        return speechReportRepository
                .findTopByInterviewAnswer_IdOrderByCreatedAtDesc(answer.getId())
                .map(this::toSpeechReport)
                .orElseGet(() -> {
                    Long interviewId = answer.getInterviewIdSafely();

                    if (interviewId == null) {
                        return null;
                    }

                    return speechReportRepository
                            .findTopByInterview_IdOrderByCreatedAtDesc(interviewId)
                            .map(this::toSpeechReport)
                            .orElse(null);
                });
    }

    private InterviewReportResponse.SpeechReport toSpeechReport(
            SpeechReportEntity report
    ) {
        return InterviewReportResponse.SpeechReport.builder()
                .reportId(report.getId())
                .textScript(report.getTextScript())
                .totalFillerCount(report.getTotalFillerCount())
                .fillerDetailsJson(report.getFillerDetailsJson())
                .repeatedExpressionsJson(report.getRepeatedExpressionsJson())
                .paceEvaluation(report.getPaceEvaluation())
                .paceFeedback(report.getPaceFeedback())
                .charactersPerSecond(report.getCharactersPerSecond())
                .totalSilenceSeconds(report.getTotalSilenceSeconds())
                .pauseRatePercent(report.getPauseRatePercent())
                .criticalSilenceCount(report.getCriticalSilenceCount())
                .volumeDbMean(report.getVolumeDbMean())
                .volumeDbStd(report.getVolumeDbStd())
                .aiEvaluationJson(report.getAiEvaluationJson())
                .modelAnswer(report.getModelAnswer())
                .build();
    }

    private String determineOverallStatus(
            List<InterviewAnswerEntity> answers
    ) {
        if (answers.isEmpty()) {
            return "EMPTY";
        }

        boolean hasFailed = answers.stream()
                .anyMatch(answer -> answer.getAnalysisStatus() == AnalysisStatus.FAILED);

        if (hasFailed) {
            return "FAILED";
        }

        boolean allDone = answers.stream()
                .allMatch(answer -> answer.getAnalysisStatus() == AnalysisStatus.DONE);

        if (allDone) {
            return "DONE";
        }

        boolean hasRunning = answers.stream()
                .anyMatch(answer -> answer.getAnalysisStatus() == AnalysisStatus.RUNNING);

        if (hasRunning) {
            return "RUNNING";
        }

        return "UPLOADED";
    }

    private UserEntity findOrCreateUser(Long userId) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 유저를 찾을 수 없습니다. userId=" + userId
                    ));
        }

        Long tempKakaoId = Math.abs(UUID.randomUUID().getMostSignificantBits());

        UserEntity tempUser = UserEntity.builder()
                .kakaoId(tempKakaoId)
                .email(null)
                .name("임시 사용자")
                .build();

        return userRepository.save(tempUser);
    }
}