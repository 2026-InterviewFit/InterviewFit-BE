package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReportEntity;
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
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    /**
     * 면접 시작.
     * userId는 카카오 로그인된 사용자 ID (JWT에서 추출된 값).
     */
    @Transactional
    public InterviewStartResponse startInterview(Long userId, InterviewStartRequest request) {
        if (userId == null) {
            throw new IllegalStateException("인증된 사용자 정보가 필요합니다.");
        }
        if (request.getJobRoleId() == null) {
            throw new IllegalArgumentException("jobRoleId는 필수입니다.");
        }

        UserEntity user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalStateException(
                        "인증된 사용자를 찾을 수 없습니다. userId=" + userId
                ));

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

    /**
     * 대시보드 면접 분석 결과 조회.
     * 요청한 사용자가 이 면접의 소유자인지 검증한다.
     */
    @Transactional(readOnly = true)
    public InterviewReportResponse getInterviewReport(Long userId, Long interviewId) {
        InterviewEntity interview = interviewRepository
                .findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 면접을 찾을 수 없습니다. interviewId=" + interviewId
                ));

        // 🔐 소유권 검증
        if (!interview.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("본인의 면접만 조회할 수 있습니다.");
        }

        List<InterviewAnswerEntity> answers =
                interviewAnswerRepository.findByInterview_IdOrderByAnswerOrderAsc(interviewId);

        InterviewReportResponse.EyeReport eyeReport =
                eyeBlinkingReportRepository
                        .findTopByInterview_IdOrderByCreatedAtDesc(interviewId)
                        .map(this::toEyeReport)
                        .orElse(null);

        InterviewReportResponse.PoseReport poseReport =
                poseReportRepository
                        .findTopByInterview_IdOrderByIdDesc(interviewId)
                        .map(this::toPoseReport)
                        .orElse(null);

        List<InterviewReportResponse.AnswerReport> answerReports =
                answers.stream()
                        .map(this::toAnswerReport)
                        .toList();

        String overallStatus = determineOverallStatus(answers);

        boolean completed = !answers.isEmpty()
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
                .eye(eyeReport)
                .pose(poseReport)
                .answers(answerReports)
                .build();
    }

    /**
     * 본인의 면접 세션 목록 조회.
     */
    @Transactional(readOnly = true)
    public List<InterviewEntity> getMyInterviews(Long userId) {
        return interviewRepository.findByUserId(userId);
    }

    // ============================
    // 아래 toXxxReport, determineOverallStatus 등은 기존 그대로 유지
    // (생략)
    // ============================

    private InterviewReportResponse.AnswerReport toAnswerReport(InterviewAnswerEntity answer) {
        return InterviewReportResponse.AnswerReport.builder()
                .answerId(answer.getId())
                .questionId(answer.getQuestion() == null ? null : answer.getQuestion().getId())
                .questionText(answer.getQuestion() == null ? null : answer.getQuestion().getQuestion())
                .answerText(answer.getAnswerText())
                .answerOrder(answer.getAnswerOrder())
                .startTime(answer.getStartTime())
                .endTime(answer.getEndTime())
                .analysisStatus(answer.getAnalysisStatus())
                .errorMessage(answer.getErrorMessage())
                .gaze(toGazeReport(answer))
                .speech(toSpeechReport(answer))
                .build();
    }

    private InterviewReportResponse.EyeReport toEyeReport(EyeBlinkingReportEntity report) {
        return InterviewReportResponse.EyeReport.builder()
                .reportId(report.getId())
                .durationSeconds(report.getDurationSeconds())
                .blinkCount(report.getBlinkCount())
                .blinkRate(report.getBlinkRate())
                .notDetectedCount(report.getNotDetectedCount())
                .notDetectedRatio(report.getNotDetectedRatio())
                .score(report.getScore())
                .grade(report.getGrade() == null ? null : report.getGrade().name())
                .feedback(report.getFeedback())
                .build();
    }

    private InterviewReportResponse.GazeReport toGazeReport(
            InterviewAnswerEntity answer
    ) {
        return eyeGazeReportRepository
                .findTopByInterview_IdOrderByCreatedAtDesc(
                        answer.getInterview().getId()
                )
                .map(this::toGazeReport)
                .orElse(null);
    }

    private InterviewReportResponse.GazeReport toGazeReport(EyeGazeReport report) {
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

    private InterviewReportResponse.PoseReport toPoseReport(PoseReportEntity report) {
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
                .findByInterview_Id(answer.getInterview().getId())
                .map(this::toSpeechReport)
                .orElse(null);
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
                .build();
    }

    private String determineOverallStatus(List<InterviewAnswerEntity> answers) {
        if (answers.isEmpty()) return "EMPTY";
        boolean hasFailed = answers.stream()
                .anyMatch(a -> a.getAnalysisStatus() == AnalysisStatus.FAILED);
        if (hasFailed) return "FAILED";
        boolean allDone = answers.stream()
                .allMatch(a -> a.getAnalysisStatus() == AnalysisStatus.DONE);
        if (allDone) return "DONE";
        boolean hasRunning = answers.stream()
                .anyMatch(a -> a.getAnalysisStatus() == AnalysisStatus.RUNNING);
        if (hasRunning) return "RUNNING";
        return "UPLOADED";
    }
}