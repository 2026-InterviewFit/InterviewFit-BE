package com.example.interviewfitbe.domain.interview.service;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReportEntity;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import com.example.interviewfitbe.domain.eye_gaze.entity.EyeGazeReport;
import com.example.interviewfitbe.domain.eye_gaze.repository.EyeGazeReportRepository;
import com.example.interviewfitbe.domain.followup.entity.FollowUpAnswerEntity;
import com.example.interviewfitbe.domain.followup.entity.FollowUpQuestionEntity;
import com.example.interviewfitbe.domain.followup.repository.FollowUpAnswerRepository;
import com.example.interviewfitbe.domain.followup.repository.FollowUpQuestionRepository;
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
import com.example.interviewfitbe.domain.speech_coaching.entity.AiEvaluationEntity;
import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import com.example.interviewfitbe.domain.speech_coaching.repository.AiEvaluationRepository;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import com.example.interviewfitbe.domain.speech_coaching.service.ExternalAiAnalysisJsonCodec;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import com.example.interviewfitbe.global.exception.BaseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
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

    private final FollowUpQuestionRepository followUpQuestionRepository;

    private final FollowUpAnswerRepository followUpAnswerRepository;

    /**
     * Gemini 내용 평가 조회용.
     *
     * ai_evaluation은 interview_answer와 1:1로 연결되어 있다.
     */
    private final AiEvaluationRepository aiEvaluationRepository;

    /**
     * AiEvaluationEntity의 JSON 문자열을
     * FE가 기대하는 aiEvaluationJson 구조로 조립할 때 사용한다.
     */
    private final ObjectMapper objectMapper;

    private final ExternalAiAnalysisJsonCodec externalAiAnalysisJsonCodec;


    /**
     * 면접 시작.
     */
    @Transactional
    public InterviewStartResponse startInterview(
            Long userId,
            InterviewStartRequest request
    ) {

        if (userId == null) {

            throw new IllegalStateException(
                    "인증된 사용자 정보가 필요합니다."
            );
        }

        if (
                request == null
                        || request.getJobRoleId() == null
        ) {

            throw new IllegalArgumentException(
                    "jobRoleId는 필수입니다."
            );
        }

        UserEntity user =
                userRepository
                        .findById(
                                userId
                        )
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "인증된 사용자를 찾을 수 없습니다. userId="
                                                + userId
                                )
                        );

        JobRoleEntity jobRole =
                jobRoleRepository
                        .findById(
                                request.getJobRoleId()
                        )
                        .orElseThrow(
                                () -> new BaseException(
                                        JobRoleErrorCode.JOB_ROLE_NOT_FOUND
                                )
                        );

        InterviewEntity interview =
                InterviewEntity.builder()
                        .user(user)
                        .jobRole(jobRole)
                        .build();

        InterviewEntity savedInterview =
                interviewRepository.save(
                        interview
                );

        return InterviewStartResponse.builder()
                .interviewId(savedInterview.getId())
                .userId(user.getId())
                .jobRoleId(jobRole.getId())
                .jobName(jobRole.getName())
                .status("STARTED")
                .build();
    }


    /**
     * 면접 분석 결과 조회.
     *
     * 최상위:
     * report.eye
     * report.pose
     * report.gaze
     *
     * 질문별:
     * report.answers[].gaze
     * report.answers[].speech
     */
    @Transactional(readOnly = true)
    public InterviewReportResponse getInterviewReport(
            Long userId,
            Long interviewId
    ) {

        InterviewEntity interview =
                interviewRepository
                        .findById(interviewId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "해당 면접을 찾을 수 없습니다. interviewId="
                                                + interviewId
                                )
                        );

        if (!interview.getUser().getId().equals(userId)) {

            throw new AccessDeniedException(
                    "본인의 면접만 조회할 수 있습니다."
            );
        }

        /*
         * interview_answers는 두 종류 모두 실제 분석 대상이다.
         *
         * 1. 질문별 Answer
         * 2. 전체 면접 영상 Answer
         */
        List<InterviewAnswerEntity> allAnswers =
                interviewAnswerRepository
                        .findByInterview_IdOrderByAnswerOrderAsc(
                                interviewId
                        );

        List<InterviewAnswerEntity> questionAnswers =
                allAnswers.stream()
                        .filter(this::isQuestionAnswer)
                        .toList();

        List<InterviewAnswerEntity> fullInterviewAnswers =
                allAnswers.stream()
                        .filter(this::isFullInterviewVideoAnswer)
                        .toList();

        /*
         * 기존 데이터 호환.
         */
        List<InterviewAnswerEntity> reportAnswers =
                questionAnswers.isEmpty()
                        ? fullInterviewAnswers
                        : questionAnswers;

        /*
         * 눈 깜빡임 분석.
         */
        InterviewReportResponse.EyeReport eyeReport =
                eyeBlinkingReportRepository
                        .findTopByInterview_IdOrderByCreatedAtDesc(
                                interviewId
                        )
                        .map(this::toEyeReport)
                        .orElse(null);

        /*
         * 자세 분석.
         */
        InterviewReportResponse.PoseReport poseReport =
                poseReportRepository
                        .findTopByInterview_IdOrderByIdDesc(
                                interviewId
                        )
                        .map(this::toPoseReport)
                        .orElse(null);

        /*
         * 시선 분석.
         *
         * 동일 면접에 분석 결과가 여러 개 존재할 수 있으므로
         * 가장 최근 createdAt 기준 결과를 사용한다.
         */
        InterviewReportResponse.GazeReport gazeReport =
                eyeGazeReportRepository
                        .findTopByInterview_IdOrderByCreatedAtDesc(
                                interviewId
                        )
                        .map(this::toGazeReport)
                        .orElse(null);

        /*
         * Speech 분석.
         */
        SpeechReportEntity speechReportEntity =
                speechReportRepository
                        .findByInterview_Id(
                                interviewId
                        )
                        .orElse(null);

        /*
         * 질문별 Answer 리포트.
         */
        List<InterviewReportResponse.AnswerReport> answerReports =
                reportAnswers.stream()
                        .map(
                                answer -> {

                                    AiEvaluationEntity aiEvaluation =
                                            aiEvaluationRepository
                                                    .findByInterviewAnswer_Id(
                                                            answer.getId()
                                                    )
                                                    .orElse(null);

                                    InterviewReportResponse.SpeechReport speechReport =
                                            speechReportEntity == null
                                                    ? null
                                                    : toSpeechReport(
                                                    speechReportEntity,
                                                    aiEvaluation
                                            );

                                    return toAnswerReport(
                                            answer,
                                            gazeReport,
                                            speechReport,
                                            aiEvaluation
                                    );
                                }
                        )
                        .toList();

        /*
         * 분석 대상 전체.
         */
        List<InterviewAnswerEntity> analysisTargets =
                new ArrayList<>();

        analysisTargets.addAll(
                questionAnswers
        );

        analysisTargets.addAll(
                fullInterviewAnswers
        );

        String overallStatus =
                determineOverallStatus(
                        analysisTargets
                );

        boolean completed =
                !fullInterviewAnswers.isEmpty()
                        && !analysisTargets.isEmpty()
                        && analysisTargets.stream()
                        .allMatch(
                                answer ->
                                        answer.getAnalysisStatus()
                                                == AnalysisStatus.DONE
                        );

        JobRoleEntity jobRole =
                interview.getJobRole();

        /*
         * 최종 응답.
         *
         * gazeReport를 최상위에도 반드시 포함한다.
         */
        return InterviewReportResponse.builder()

                .interviewId(
                        interview.getId()
                )

                .userId(
                        interview.getUser().getId()
                )

                .jobRoleId(
                        jobRole.getId()
                )

                .jobCategory(
                        jobRole.getCategory()
                )

                .jobName(
                        jobRole.getName()
                )

                .createdAt(
                        interview.getCreatedAt()
                )

                .analysisStatus(
                        overallStatus
                )

                .completed(
                        completed
                )

                .eye(
                        eyeReport
                )

                .pose(
                        poseReport
                )

                .gaze(
                        gazeReport
                )

                .answers(
                        answerReports
                )

                .build();
    }


    /**
     * 현재 로그인 사용자의 면접 목록.
     */
    @Transactional(readOnly = true)
    public List<InterviewEntity> getMyInterviews(
            Long userId
    ) {

        return interviewRepository
                .findByUserId(
                        userId
                );
    }


    /**
     * 질문별 결과 DTO.
     */
    private InterviewReportResponse.AnswerReport toAnswerReport(
            InterviewAnswerEntity answer,
            InterviewReportResponse.GazeReport gazeReport,
            InterviewReportResponse.SpeechReport speechReport,
            AiEvaluationEntity aiEvaluation
    ) {

        return InterviewReportResponse.AnswerReport.builder()

                .answerId(
                        answer.getId()
                )

                .interviewQuestionId(
                        answer.getInterviewQuestionId()
                )

                .questionId(
                        answer.getQuestionId()
                )

                .questionText(
                        answer.getQuestionText()
                )

                .videoObjectKey(
                        answer.getVideoObjectKey()
                )

                .answerText(
                        answer.getAnswerText()
                )

                .answerOrder(
                        answer.getAnswerOrder()
                )

                .startTime(
                        answer.getStartTime()
                )

                .endTime(
                        answer.getEndTime()
                )

                .analysisStatus(
                        answer.getAnalysisStatus()
                )

                .errorMessage(
                        answer.getErrorMessage()
                )

                .aiEvaluation(
                        toAiEvaluationReport(
                                aiEvaluation
                        )
                )

                .followUps(
                        toFollowUpReports(
                                answer
                        )
                )

                .gaze(
                        gazeReport
                )

                .speech(
                        speechReport
                )

                .build();
    }


    private InterviewReportResponse.AiEvaluationReport toAiEvaluationReport(
            AiEvaluationEntity aiEvaluation
    ) {

        if (aiEvaluation == null) {
            return null;
        }

        return InterviewReportResponse.AiEvaluationReport.builder()

                .evaluationId(
                        aiEvaluation.getId()
                )

                .starEvaluationJson(
                        aiEvaluation.getStarEvaluationJson()
                )

                .geminiFeedback(
                        aiEvaluation.getGeminiFeedback()
                )

                .keywordsJson(
                        aiEvaluation.getKeywordsJson()
                )

                .modelAnswer(
                        aiEvaluation.getModelAnswer()
                )

                .build();
    }


    private List<InterviewReportResponse.FollowUpReport> toFollowUpReports(
            InterviewAnswerEntity sourceAnswer
    ) {

        if (
                sourceAnswer == null
                        || sourceAnswer.getId() == null
        ) {

            return List.of();
        }

        return followUpQuestionRepository
                .findBySourceAnswer_IdOrderByCreatedAtAsc(
                        sourceAnswer.getId()
                )
                .stream()
                .map(
                        this::toFollowUpReport
                )
                .toList();
    }


    private InterviewReportResponse.FollowUpReport toFollowUpReport(
            FollowUpQuestionEntity followUpQuestion
    ) {

        List<InterviewReportResponse.FollowUpAnswerReport> answers =
                followUpAnswerRepository
                        .findByFollowUpQuestion_IdOrderByCreatedAtAsc(
                                followUpQuestion.getId()
                        )
                        .stream()
                        .map(
                                this::toFollowUpAnswerReport
                        )
                        .toList();

        return InterviewReportResponse.FollowUpReport.builder()

                .followUpQuestionId(
                        followUpQuestion.getId()
                )

                .questionText(
                        followUpQuestion.getFollowUpQuestion()
                )

                .createdAt(
                        followUpQuestion.getCreatedAt()
                )

                .answers(
                        answers
                )

                .build();
    }


    private InterviewReportResponse.FollowUpAnswerReport toFollowUpAnswerReport(
            FollowUpAnswerEntity answer
    ) {

        return InterviewReportResponse.FollowUpAnswerReport.builder()

                .followUpAnswerId(
                        answer.getId()
                )

                .answerText(
                        answer.getAnswerText()
                )

                .startTime(
                        answer.getStartTime()
                )

                .endTime(
                        answer.getEndTime()
                )

                .analysisStatus(
                        answer.getAnalysisStatus()
                )

                .errorMessage(
                        answer.getErrorMessage()
                )

                .build();
    }


    /**
     * 눈 깜빡임 DTO 변환.
     */
    private InterviewReportResponse.EyeReport toEyeReport(
            EyeBlinkingReportEntity report
    ) {

        return InterviewReportResponse.EyeReport.builder()

                .reportId(
                        report.getId()
                )

                .durationSeconds(
                        report.getDurationSeconds()
                )

                .blinkCount(
                        report.getBlinkCount()
                )

                .blinkRate(
                        report.getBlinkRate()
                )

                .notDetectedCount(
                        report.getNotDetectedCount()
                )

                .notDetectedRatio(
                        report.getNotDetectedRatio()
                )

                .score(
                        report.getScore()
                )

                .grade(
                        report.getGrade() == null
                                ? null
                                : report.getGrade().name()
                )

                .feedback(
                        report.getFeedback()
                )

                .build();
    }


    /**
     * 시선 분석 DTO 변환.
     */
    private InterviewReportResponse.GazeReport toGazeReport(
            EyeGazeReport report
    ) {

        return InterviewReportResponse.GazeReport.builder()

                .reportId(
                        report.getId()
                )

                .centerRatio(
                        report.getCenterRatio()
                )

                .gazeAwayRatio(
                        report.getGazeAwayRatio()
                )

                .gazeAwayCount(
                        report.getGazeAwayCount()
                )

                .score(
                        report.getScore()
                )

                .evaluation(
                        report.getEvaluation()
                )

                .resultJson(
                        report.getResultJson()
                )

                .build();
    }


    /**
     * 자세 분석 DTO 변환.
     */
    private InterviewReportResponse.PoseReport toPoseReport(
            PoseReportEntity report
    ) {

        return InterviewReportResponse.PoseReport.builder()

                .reportId(
                        report.getId()
                )

                .totalDuration(
                        report.getTotalDuration()
                )

                .badCount(
                        report.getBadCount()
                )

                .badDuration(
                        report.getBadDuration()
                )

                .shoulderBadCount(
                        report.getShoulderBadCount()
                )

                .faceBadCount(
                        report.getFaceBadCount()
                )

                .shoulderBadDuration(
                        report.getShoulderBadDuration()
                )

                .faceBadDuration(
                        report.getFaceBadDuration()
                )

                .badRatio(
                        report.getBadRatio()
                )

                .score(
                        report.getScore()
                )

                .grade(
                        report.getGrade() == null
                                ? null
                                : report.getGrade().name()
                )

                .build();
    }


    /**
     * SpeechReportEntity + AiEvaluationEntity를
     * FE가 기대하는 하나의 SpeechReport DTO로 합친다.
     */
    private InterviewReportResponse.SpeechReport toSpeechReport(
            SpeechReportEntity report,
            AiEvaluationEntity aiEvaluation
    ) {

        return InterviewReportResponse.SpeechReport.builder()

                .reportId(
                        report.getId()
                )

                .speechScore(
                        report.getScore()
                )

                .speechScoreStatus(
                        report.getSpeechScoreStatus()
                )

                .speechScoreReason(
                        report.getSpeechScoreReason()
                )

                .textScript(
                        report.getTextScript()
                )

                .totalFillerCount(
                        report.getTotalFillerCount()
                )

                .fillerDetailsJson(
                        report.getFillerDetailsJson()
                )

                .repeatedExpressionsJson(
                        report.getRepeatedExpressionsJson()
                )

                .paceEvaluation(
                        report.getPaceEvaluation()
                )

                .paceFeedback(
                        report.getPaceFeedback()
                )

                .charactersPerSecond(
                        report.getCharactersPerSecond()
                )

                .totalSilenceSeconds(
                        report.getTotalSilenceSeconds()
                )

                .pauseRatePercent(
                        report.getPauseRatePercent()
                )

                .criticalSilenceCount(
                        report.getCriticalSilenceCount()
                )

                .volumeDbMean(
                        null
                )

                .volumeDbStd(
                        null
                )

                .aiEvaluationJson(
                        toAiEvaluationJson(
                                aiEvaluation
                        )
                )

                .modelAnswer(
                        aiEvaluation == null
                                ? null
                                : aiEvaluation.getModelAnswer()
                )

                .externalAiAnalysis(
                        aiEvaluation == null
                                ? null
                                : externalAiAnalysisJsonCodec.deserializeSafely(
                                aiEvaluation.getExternalAiAnalysisJson()
                        )
                )

                .build();
    }


    /**
     * AI 평가 JSON 변환.
     */
    private String toAiEvaluationJson(
            AiEvaluationEntity aiEvaluation
    ) {

        if (aiEvaluation == null) {
            return null;
        }

        ObjectNode root =
                objectMapper.createObjectNode();

        /*
         * STAR 점수.
         */
        String starJson =
                aiEvaluation.getStarEvaluationJson();

        if (
                starJson != null
                        && !starJson.isBlank()
        ) {

            try {

                root.set(
                        "star_score",
                        objectMapper.readTree(
                                starJson
                        )
                );

            } catch (JsonProcessingException e) {

                log.warn(
                        "STAR 평가 JSON 파싱 실패: aiEvaluationId={}",
                        aiEvaluation.getId()
                );

                root.putObject(
                        "star_score"
                );
            }

        } else {

            root.putObject(
                    "star_score"
            );
        }

        /*
         * Gemini 피드백.
         */
        String feedback =
                aiEvaluation.getGeminiFeedback();

        if (feedback != null) {

            root.put(
                    "feedback",
                    feedback
            );

        } else {

            root.putNull(
                    "feedback"
            );
        }

        /*
         * 키워드.
         */
        String keywordsJson =
                aiEvaluation.getKeywordsJson();

        if (
                keywordsJson != null
                        && !keywordsJson.isBlank()
        ) {

            try {

                root.set(
                        "keywords",
                        objectMapper.readTree(
                                keywordsJson
                        )
                );

            } catch (JsonProcessingException e) {

                log.warn(
                        "키워드 JSON 파싱 실패: aiEvaluationId={}",
                        aiEvaluation.getId()
                );

                root.putArray(
                        "keywords"
                );
            }

        } else {

            root.putArray(
                    "keywords"
            );
        }

        return root.toString();
    }


    /**
     * 질문별 Answer 판별.
     */
    private boolean isQuestionAnswer(
            InterviewAnswerEntity answer
    ) {

        return answer.getPtQuestion() == null
                && (
                answer.getInterviewQuestion() != null
                        || answer.getQuestion() != null
        );
    }


    /**
     * 전체 면접 영상 Answer 판별.
     */
    private boolean isFullInterviewVideoAnswer(
            InterviewAnswerEntity answer
    ) {

        return answer.getInterviewQuestion() == null
                && answer.getQuestion() == null
                && answer.getPtQuestion() == null;
    }


    /**
     * 전체 분석 상태 계산.
     */
    private String determineOverallStatus(
            List<InterviewAnswerEntity> answers
    ) {

        if (answers.isEmpty()) {
            return "EMPTY";
        }

        boolean hasFailed =
                answers.stream()
                        .anyMatch(
                                answer ->
                                        answer.getAnalysisStatus()
                                                == AnalysisStatus.FAILED
                        );

        if (hasFailed) {
            return "FAILED";
        }

        boolean allDone =
                answers.stream()
                        .allMatch(
                                answer ->
                                        answer.getAnalysisStatus()
                                                == AnalysisStatus.DONE
                        );

        if (allDone) {
            return "DONE";
        }

        boolean hasRunning =
                answers.stream()
                        .anyMatch(
                                answer ->
                                        answer.getAnalysisStatus()
                                                == AnalysisStatus.RUNNING
                        );

        if (hasRunning) {
            return "RUNNING";
        }

        return "UPLOADED";
    }
}