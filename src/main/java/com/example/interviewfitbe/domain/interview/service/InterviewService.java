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
import com.example.interviewfitbe.domain.speech_coaching.entity.AiEvaluationEntity;
import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import com.example.interviewfitbe.domain.speech_coaching.repository.AiEvaluationRepository;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
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


    /**
     * 면접 시작.
     *
     * userId는 JWT 인증 사용자 ID를 사용한다.
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

                        .user(
                                user
                        )

                        .jobRole(
                                jobRole
                        )

                        .build();


        InterviewEntity savedInterview =
                interviewRepository.save(
                        interview
                );


        return InterviewStartResponse.builder()

                .interviewId(
                        savedInterview.getId()
                )

                .userId(
                        user.getId()
                )

                .jobRoleId(
                        jobRole.getId()
                )

                .jobName(
                        jobRole.getName()
                )

                .status(
                        "STARTED"
                )

                .build();
    }


    /**
     * 면접 분석 결과 조회.
     *
     * 현재 데이터 위치:
     *
     * report.eye
     * report.pose
     *
     * report.answers[].gaze
     * report.answers[].speech
     *
     * speech 안의 Gemini 내용 평가는
     * ai_evaluation 테이블에서 answerId 기준으로 조회하여
     * aiEvaluationJson / modelAnswer로 합쳐서 내려준다.
     */
    @Transactional(readOnly = true)
    public InterviewReportResponse getInterviewReport(
            Long userId,
            Long interviewId
    ) {

        InterviewEntity interview =
                interviewRepository
                        .findById(
                                interviewId
                        )
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "해당 면접을 찾을 수 없습니다. interviewId="
                                                + interviewId
                                )
                        );


        if (
                !interview
                        .getUser()
                        .getId()
                        .equals(
                                userId
                        )
        ) {

            throw new AccessDeniedException(
                    "본인의 면접만 조회할 수 있습니다."
            );
        }


        List<InterviewAnswerEntity> answers =
                interviewAnswerRepository
                        .findByInterview_IdOrderByAnswerOrderAsc(
                                interviewId
                        );


        /*
         * =====================================================
         * Eye
         * =====================================================
         */
        InterviewReportResponse.EyeReport eyeReport =
                eyeBlinkingReportRepository
                        .findTopByInterview_IdOrderByCreatedAtDesc(
                                interviewId
                        )
                        .map(
                                this::toEyeReport
                        )
                        .orElse(
                                null
                        );


        /*
         * =====================================================
         * Pose
         * =====================================================
         */
        InterviewReportResponse.PoseReport poseReport =
                poseReportRepository
                        .findTopByInterview_IdOrderByIdDesc(
                                interviewId
                        )
                        .map(
                                this::toPoseReport
                        )
                        .orElse(
                                null
                        );


        /*
         * =====================================================
         * Gaze
         * =====================================================
         *
         * 현재 gaze report는 interview 단위 최신 리포트를 조회한다.
         */
        InterviewReportResponse.GazeReport gazeReport =
                eyeGazeReportRepository
                        .findTopByInterview_IdOrderByCreatedAtDesc(
                                interviewId
                        )
                        .map(
                                this::toGazeReport
                        )
                        .orElse(
                                null
                        );


        /*
         * =====================================================
         * Speech
         * =====================================================
         *
         * speech_report는 현재 interview 하나당 하나다.
         * Gemini ai_evaluation은 answer 하나당 하나다.
         */
        SpeechReportEntity speechReportEntity =
                speechReportRepository
                        .findByInterview_Id(
                                interviewId
                        )
                        .orElse(
                                null
                        );


        /*
         * 각 answer마다 해당 answer의 AI 평가를 찾아
         * speech DTO에 합쳐서 반환한다.
         */
        List<InterviewReportResponse.AnswerReport> answerReports =
                answers.stream()

                        .map(
                                answer -> {

                                    AiEvaluationEntity aiEvaluation =
                                            aiEvaluationRepository
                                                    .findByInterviewAnswer_Id(
                                                            answer.getId()
                                                    )
                                                    .orElse(
                                                            null
                                                    );


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
                                            speechReport
                                    );
                                }
                        )

                        .toList();


        String overallStatus =
                determineOverallStatus(
                        answers
                );


        boolean completed =
                !answers.isEmpty()

                        && answers.stream()
                        .allMatch(
                                answer ->
                                        answer.getAnalysisStatus()
                                                == AnalysisStatus.DONE
                        );


        JobRoleEntity jobRole =
                interview.getJobRole();


        return InterviewReportResponse.builder()

                .interviewId(
                        interview.getId()
                )

                .userId(
                        interview
                                .getUser()
                                .getId()
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


    private InterviewReportResponse.AnswerReport toAnswerReport(
            InterviewAnswerEntity answer,
            InterviewReportResponse.GazeReport gazeReport,
            InterviewReportResponse.SpeechReport speechReport
    ) {

        return InterviewReportResponse.AnswerReport.builder()

                .answerId(
                        answer.getId()
                )

                .questionId(
                        answer.getQuestionId()
                )

                .questionText(
                        answer.getQuestionText()
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

                .gaze(
                        gazeReport
                )

                .speech(
                        speechReport
                )

                .build();
    }


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

                /*
                 * 현재 SpeechReportEntity에는 volume 필드가 없으므로
                 * DTO에는 null로 내려간다.
                 */
                .volumeDbMean(
                        null
                )

                .volumeDbStd(
                        null
                )

                /*
                 * FE SpeechAnalysisCard / reportUtils가
                 * JSON.parse(aiEvaluationJson)를 사용하므로
                 * 반드시 JSON object 문자열로 내려준다.
                 */
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

                .build();
    }


    /**
     * ai_evaluation 테이블은 다음처럼 컬럼이 분리되어 있다.
     *
     * starEvaluationJson : {"S": 8, "T": 7, "A": 9, "R": 6}
     * geminiFeedback     : "..."
     * keywordsJson       : ["Spring Boot", "PostgreSQL"]
     *
     * FE는 아래 형태를 기대한다.
     *
     * {
     *   "star_score": {...},
     *   "feedback": "...",
     *   "keywords": [...]
     * }
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
         * STAR 점수
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
         * Gemini 피드백
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
         * 키워드
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