package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReport;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
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
import com.example.interviewfitbe.domain.pose.entity.PoseAnalysisResultEntity;
import com.example.interviewfitbe.domain.pose.repository.PoseAnalysisResultRepository;
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
    private final PoseAnalysisResultRepository poseAnalysisResultRepository;
    private final SpeechReportRepository speechReportRepository;

    /**
     * 면접 시작 API에서 호출하는 메서드.
     *
     * 역할:
     * 1. userId가 있으면 기존 유저 조회
     * 2. userId가 없으면 임시 유저 생성
     * 3. jobRoleId로 직무 조회
     * 4. interview row 생성
     * 5. FE에 interviewId 반환
     */
    @Transactional
    public InterviewStartResponse startInterview(InterviewStartRequest request) {
        if (request.getJobRoleId() == null) {
            throw new IllegalArgumentException("jobRoleId는 필수입니다.");
        }

        UserEntity user = findOrCreateUser(request.getUserId());

        JobRoleEntity jobRole = jobRoleRepository.findById(request.getJobRoleId())
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
     * 대시보드 분석 결과 조회.
     *
     * interviewId 기준으로 질문별 답변 목록을 가져오고,
     * 각 answerId에 연결된 eye / pose / speech 결과를 붙여서 반환한다.
     */
    @Transactional(readOnly = true)
    public InterviewReportResponse getInterviewReport(Long interviewId) {
        InterviewEntity interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "해당 면접을 찾을 수 없습니다. interviewId=" + interviewId
                ));

        List<InterviewAnswerEntity> answers =
                interviewAnswerRepository.findByInterview_IdOrderByAnswerOrderAsc(interviewId);

        List<InterviewReportResponse.AnswerReport> answerReports = answers.stream()
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
                .answers(answerReports)
                .build();
    }

    private InterviewReportResponse.AnswerReport toAnswerReport(InterviewAnswerEntity answer) {
        return InterviewReportResponse.AnswerReport.builder()
                .answerId(answer.getId())
                .questionId(answer.getQuestionId())
                .questionText(answer.getQuestionText())
                .videoObjectKey(answer.getVideoObjectKey())
                .answerOrder(answer.getAnswerOrder())
                .analysisStatus(answer.getAnalysisStatus())
                .errorMessage(answer.getErrorMessage())
                .eye(toEyeReport(answer))
                .pose(toPoseReport(answer))
                .speech(toSpeechReport(answer))
                .build();
    }

    private InterviewReportResponse.EyeReport toEyeReport(InterviewAnswerEntity answer) {
        return eyeBlinkingReportRepository.findTopByInterviewAnswer_IdOrderByCreatedAtDesc(answer.getId())
                .map(this::toEyeReport)
                .orElse(null);
    }

    private InterviewReportResponse.EyeReport toEyeReport(EyeBlinkingReport report) {
        return InterviewReportResponse.EyeReport.builder()
                .reportId(report.getId())
                .blinkCount(report.getBlinkCount())
                .blinkRate(report.getBlinkRate())
                .score(report.getScore())
                .feedback(report.getFeedback())
                .build();
    }

    private InterviewReportResponse.PoseReport toPoseReport(InterviewAnswerEntity answer) {
        return poseAnalysisResultRepository.findTopByInterviewAnswer_IdOrderByIdDesc(answer.getId())
                .map(this::toPoseReport)
                .orElse(null);
    }

    private InterviewReportResponse.PoseReport toPoseReport(PoseAnalysisResultEntity report) {
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

    private InterviewReportResponse.SpeechReport toSpeechReport(InterviewAnswerEntity answer) {
        return speechReportRepository.findTopByInterviewAnswer_IdOrderByCreatedAtDesc(answer.getId())
                .map(this::toSpeechReport)
                .orElse(null);
    }

    private InterviewReportResponse.SpeechReport toSpeechReport(SpeechReportEntity report) {
        return InterviewReportResponse.SpeechReport.builder()
                .reportId(report.getId())
                .textScript(report.getTextScript())
                .totalFillerCount(report.getTotalFillerCount())
                .fillerDetailsJson(report.getFillerDetailsJson())
                .paceEvaluation(report.getPaceEvaluation())
                .paceFeedback(report.getPaceFeedback())
                .charactersPerSecond(report.getCharactersPerSecond())
                .totalSilenceSeconds(report.getTotalSilenceSeconds())
                .criticalSilenceCount(report.getCriticalSilenceCount())
                .aiEvaluationJson(report.getAiEvaluationJson())
                .modelAnswer(report.getModelAnswer())
                .build();
    }

    private String determineOverallStatus(List<InterviewAnswerEntity> answers) {
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

    /**
     * 로그인 기능이 완전히 붙기 전까지 안전하게 쓰는 유저 처리 메서드.
     *
     * userId가 있으면 기존 유저 사용.
     * userId가 없으면 임시 유저 생성.
     */
    private UserEntity findOrCreateUser(Long userId) {
        if (userId != null) {
            return userRepository.findById(userId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "해당 유저를 찾을 수 없습니다. userId=" + userId
                    ));
        }

        String tempEmail = "temp_" + UUID.randomUUID() + "@interviewfit.local";

        UserEntity tempUser = UserEntity.builder()
                .email(tempEmail)
                .name("임시 사용자")
                .build();

        return userRepository.save(tempUser);
    }
}