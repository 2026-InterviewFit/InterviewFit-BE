package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReportEntity;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import com.example.interviewfitbe.domain.eye_gaze.entity.EyeGazeReport;
import com.example.interviewfitbe.domain.eye_gaze.repository.EyeGazeReportRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.pose.repository.PoseReportRepository;
import com.example.interviewfitbe.domain.pt_interview.dto.*;
import com.example.interviewfitbe.domain.pt_interview.entity.PtDocumentEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtInterviewEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtQuestionEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtReportEntity;
import com.example.interviewfitbe.domain.pt_interview.repository.PtDocumentRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtInterviewRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtQuestionRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtReportRepository;
import com.example.interviewfitbe.domain.speech_coaching.entity.AiEvaluationEntity;
import com.example.interviewfitbe.domain.speech_coaching.repository.AiEvaluationRepository;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PtReportService {

    private final PtInterviewRepository ptInterviewRepository;
    private final PtDocumentRepository ptDocumentRepository;
    private final PtQuestionRepository ptQuestionRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final PtReportRepository ptReportRepository;
    private final PtReportAnalysisService ptReportAnalysisService;

    private final SpeechReportRepository speechReportRepository;
    private final EyeGazeReportRepository eyeGazeReportRepository;
    private final EyeBlinkingReportRepository eyeBlinkingReportRepository;
    private final PoseReportRepository poseReportRepository;

    /*
     * 기존 질문별 AI 평가 조회
     */
    private final AiEvaluationRepository aiEvaluationRepository;

    private final ObjectMapper objectMapper;

    /**
     * PT 종합 리포트 생성
     */
    @Transactional
    public PtReportResponse generateReport(
            Long ptInterviewId
    ) {

        /*
         * 1. PT 면접 조회
         */
        PtInterviewEntity ptInterview =
                ptInterviewRepository
                        .findById(ptInterviewId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 PT 면접입니다."
                                )
                        );

        /*
         * 2. 발표 자료 조회
         */
        PtDocumentEntity document =
                ptDocumentRepository
                        .findByPtInterviewId(ptInterviewId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "PT 발표 자료를 찾을 수 없습니다."
                                )
                        );

        /*
         * 3. PT 질문 조회
         */
        List<PtQuestionEntity> questions =
                ptQuestionRepository
                        .findAllByPtInterviewIdOrderByQuestionNumberAsc(
                                ptInterviewId
                        );

        /*
         * 4. PT 질문별 실제 사용자 답변 조회
         */
        List<InterviewAnswerEntity> answers =
                interviewAnswerRepository
                        .findAllByPtQuestion_PtInterview_IdOrderByAnswerOrderAsc(
                                ptInterviewId
                        );

        /*
         * 5. 발표자료 자체 AI 요약
         */
        String presentationMaterialSummary =
                document.getSummary() == null
                        ? ""
                        : document.getSummary();

        /*
         * 6. PT 면접과 연결된 Interview ID
         */
        if (
                ptInterview.getInterview() == null
                        || ptInterview.getInterview().getId() == null
        ) {
            throw new IllegalStateException(
                    "PT 면접에 연결된 Interview 정보를 찾을 수 없습니다."
            );
        }

        Long interviewId =
                ptInterview
                        .getInterview()
                        .getId();

        /*
         * 7. 실제 발표 영상 음성 분석 결과
         */
        var speechReport =
                speechReportRepository
                        .findByInterview_Id(
                                interviewId
                        );

        String presentationTranscript =
                speechReport
                        .map(report ->
                                report.getTextScript() == null
                                        ? ""
                                        : report.getTextScript()
                        )
                        .orElse("");

        List<Double> speechScores =
                speechReport
                        .map(report ->
                                java.util.Collections.singletonList(report.getScore())
                        )
                        .orElseGet(List::of);

        /*
         * 8. 질문 + 실제 답변 결합
         */
        List<PtQuestionAnswerRequest> qaItems =
                buildQuestionAnswerItems(
                        questions,
                        answers
                );

        /*
         * 9. 시선 점수
         */
        List<Double> eyeScores =
                extractEyeScores(
                        interviewId
                );

        /*
         * 10. 눈 깜빡임 점수
         */
        List<Double> blinkScores =
                extractBlinkScores(
                        answers
                );

        /*
         * 11. 자세 점수
         */
        List<Double> poseScores =
                extractPoseScores(
                        interviewId
                );

        /*
         * 12. FastAPI PT 종합 리포트 요청
         */
        PtReportGenerateRequest request =
                new PtReportGenerateRequest(
                        presentationMaterialSummary,
                        presentationTranscript,
                        qaItems,
                        eyeScores,
                        blinkScores,
                        poseScores,
                        speechScores
                );

        /*
         * 13. FastAPI AI 분석 실행
         */
        PtReportGenerateResponse response =
                ptReportAnalysisService
                        .generateReport(
                                request
                        );

        /*
         * 14. 질문별 상세 분석 JSON 저장
         *
         * 여기에는 기존 PT FastAPI 분석 결과만 저장한다.
         * STAR / modelAnswer / geminiFeedback은
         * 조회 시 ai_evaluation 테이블에서 결합한다.
         */
        String resultJson =
                serializeQuestionAnalyses(
                        response.questionAnalyses()
                );

        /*
         * 15. 기존 리포트 갱신 또는 신규 생성
         */
        PtReportEntity report =
                ptReportRepository
                        .findByPtInterviewId(
                                ptInterviewId
                        )
                        .map(existingReport -> {

                            existingReport.update(
                                    response.contentScore(),
                                    response.structureScore(),
                                    response.deliveryScore(),
                                    response.qaScore(),
                                    response.overallScore(),
                                    response.presentationMaterialSummary(),
                                    response.actualPresentationSummary(),
                                    response.presentationComparison(),
                                    response.strengths(),
                                    response.weaknesses(),
                                    response.improvement(),
                                    resultJson
                            );

                            return existingReport;
                        })
                        .orElseGet(() ->
                                PtReportEntity.create(
                                        ptInterview,
                                        response.contentScore(),
                                        response.structureScore(),
                                        response.deliveryScore(),
                                        response.qaScore(),
                                        response.overallScore(),
                                        response.presentationMaterialSummary(),
                                        response.actualPresentationSummary(),
                                        response.presentationComparison(),
                                        response.strengths(),
                                        response.weaknesses(),
                                        response.improvement(),
                                        resultJson
                                )
                        );

        /*
         * 16. DB 저장
         */
        PtReportEntity saved =
                ptReportRepository.save(
                        report
                );

        /*
         * 17. API 응답 변환
         */
        return toResponse(
                saved
        );
    }

    /**
     * 생성된 PT 종합 리포트 조회
     */
    @Transactional(readOnly = true)
    public PtReportResponse getReport(
            Long ptInterviewId
    ) {

        PtReportEntity report =
                ptReportRepository
                        .findByPtInterviewId(
                                ptInterviewId
                        )
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "PT 리포트가 존재하지 않습니다."
                                )
                        );

        return toResponse(
                report
        );
    }

    /**
     * PT 질문과 실제 사용자 답변을 연결한다.
     */
    private List<PtQuestionAnswerRequest> buildQuestionAnswerItems(
            List<PtQuestionEntity> questions,
            List<InterviewAnswerEntity> answers
    ) {

        List<PtQuestionAnswerRequest> result =
                new ArrayList<>();

        for (PtQuestionEntity question : questions) {

            String questionText =
                    question.getQuestion() == null
                            ? ""
                            : question.getQuestion();

            InterviewAnswerEntity matchedAnswer =
                    answers.stream()
                            .filter(answer ->
                                    answer.getPtQuestion() != null
                                            && answer
                                            .getPtQuestion()
                                            .getId() != null
                                            && answer
                                            .getPtQuestion()
                                            .getId()
                                            .equals(
                                                    question.getId()
                                            )
                            )
                            .findFirst()
                            .orElse(null);

            String answerText = "";

            if (
                    matchedAnswer != null
                            && matchedAnswer.getAnswerText() != null
            ) {
                answerText =
                        matchedAnswer
                                .getAnswerText()
                                .trim();
            }

            result.add(
                    new PtQuestionAnswerRequest(
                            question.getQuestionNumber(),
                            questionText,
                            answerText
                    )
            );
        }

        return result;
    }

    /**
     * 면접 기준 가장 최근 시선 분석 점수
     */
    private List<Double> extractEyeScores(
            Long interviewId
    ) {

        return eyeGazeReportRepository
                .findTopByInterview_IdOrderByCreatedAtDesc(
                        interviewId
                )
                .map(report -> {

                    Double score =
                            report.getScore() == null
                                    ? 0.0
                                    : report.getScore();

                    return List.of(
                            score
                    );
                })
                .orElseGet(
                        List::of
                );
    }

    /**
     * PT 질문 답변별 눈 깜빡임 점수
     */
    private List<Double> extractBlinkScores(
            List<InterviewAnswerEntity> answers
    ) {

        return answers.stream()
                .map(answer ->
                        eyeBlinkingReportRepository
                                .findTopByInterviewAnswer_IdOrderByCreatedAtDesc(
                                        answer.getId()
                                )
                                .map(report ->
                                        report.getScore() == null
                                                ? 0.0
                                                : report.getScore()
                                )
                                .orElse(
                                        0.0
                                )
                )
                .toList();
    }

    /**
     * 면접 기준 가장 최근 자세 분석 점수
     */
    private List<Double> extractPoseScores(
            Long interviewId
    ) {

        return poseReportRepository
                .findTopByInterview_IdOrderByIdDesc(
                        interviewId
                )
                .map(report -> {

                    Double score =
                            report.getScore() == null
                                    ? 0.0
                                    : report
                                    .getScore()
                                    .doubleValue();

                    return List.of(
                            score
                    );
                })
                .orElseGet(
                        List::of
                );
    }

    /**
     * 질문별 분석 결과를 JSON으로 저장
     */
    private String serializeQuestionAnalyses(
            List<PtQuestionAnalysisResponse> analyses
    ) {

        if (
                analyses == null
                        || analyses.isEmpty()
        ) {
            return null;
        }

        try {

            return objectMapper
                    .writeValueAsString(
                            analyses
                    );

        } catch (Exception e) {

            throw new IllegalStateException(
                    "PT 질문별 분석 결과 JSON 변환에 실패했습니다.",
                    e
            );
        }
    }

    /**
     * 저장된 질문별 분석 JSON 복원
     */
    private List<PtQuestionAnalysisResponse> deserializeQuestionAnalyses(
            String resultJson
    ) {

        if (
                resultJson == null
                        || resultJson.isBlank()
        ) {
            return List.of();
        }

        try {

            return objectMapper
                    .readValue(
                            resultJson,
                            new TypeReference<
                                    List<PtQuestionAnalysisResponse>
                                    >() {
                            }
                    );

        } catch (Exception e) {

            /*
             * 기존 데이터가 다른 형식이어도
             * 리포트 전체 조회는 실패시키지 않는다.
             */
            return List.of();
        }
    }

    /**
     * 발표 전체 영상 상세 분석 생성
     */
    private PtPresentationAnalysisResponse buildPresentationAnalysis(
            Long interviewId
    ) {

        /*
         * 눈 깜빡임
         *
         * 발표 영상은 interviewAnswer가 null인 결과를 사용한다.
         */
        PtEyeBlinkingDetailResponse blinking =
                eyeBlinkingReportRepository
                        .findTopByInterview_IdAndInterviewAnswerIsNullOrderByCreatedAtDesc(
                                interviewId
                        )
                        .map(this::toBlinkingDetail)
                        .orElse(null);

        /*
         * 시선
         */
        PtEyeGazeDetailResponse gaze =
                eyeGazeReportRepository
                        .findTopByInterview_IdOrderByCreatedAtDesc(
                                interviewId
                        )
                        .map(this::toGazeDetail)
                        .orElse(null);

        /*
         * 음성
         */
        PtSpeechDetailResponse speech =
                speechReportRepository
                        .findByInterview_Id(
                                interviewId
                        )
                        .map(report ->
                                new PtSpeechDetailResponse(
                                        report.getScore(),
                                        report.getScore(),
                                        report.getSpeechScoreStatus(),
                                        report.getSpeechScoreReason(),
                                        report.getTextScript(),
                                        report.getTotalFillerCount(),
                                        report.getFillerDetailsJson(),
                                        report.getRepeatedExpressionsJson(),
                                        report.getPaceEvaluation(),
                                        report.getPaceFeedback(),
                                        report.getCharactersPerSecond(),
                                        report.getTotalSilenceSeconds(),
                                        report.getPauseRatePercent(),
                                        report.getCriticalSilenceCount()
                                )
                        )
                        .orElse(null);

        return new PtPresentationAnalysisResponse(
                blinking,
                gaze,
                speech
        );
    }

    /**
     * 눈 깜빡임 Entity -> DTO
     */
    private PtEyeBlinkingDetailResponse toBlinkingDetail(
            EyeBlinkingReportEntity report
    ) {

        return new PtEyeBlinkingDetailResponse(
                report.getScore(),
                report.getDurationSeconds(),
                report.getBlinkCount(),
                report.getBlinkRate(),
                report.getNotDetectedCount(),
                report.getNotDetectedRatio(),
                report.getGrade() == null
                        ? null
                        : report.getGrade().name(),
                report.getFeedback()
        );
    }

    /**
     * 시선 Entity -> DTO
     */
    private PtEyeGazeDetailResponse toGazeDetail(
            EyeGazeReport report
    ) {

        return new PtEyeGazeDetailResponse(
                report.getScore(),
                report.getDurationSeconds(),
                report.getCenterRatio(),
                report.getGazeAwayRatio(),
                report.getGazeAwayCount(),
                report.getNotDetectedCount(),
                report.getNotDetectedRatio(),
                report.getGrade() == null
                        ? null
                        : report.getGrade().name(),
                report.getEvaluation()
        );
    }

    /**
     * 기존 PT 질문별 분석 결과와
     * ai_evaluation의 STAR / 피드백 / 모범답안을 결합한다.
     */
    private List<PtQuestionAnalysisResponse> enrichQuestionAnalyses(
            Long ptInterviewId,
            List<PtQuestionAnalysisResponse> analyses
    ) {

        if (
                analyses == null
                        || analyses.isEmpty()
        ) {
            return List.of();
        }

        /*
         * PT 질문별 실제 사용자 답변 조회
         */
        List<InterviewAnswerEntity> answers =
                interviewAnswerRepository
                        .findAllByPtQuestion_PtInterview_IdOrderByAnswerOrderAsc(
                                ptInterviewId
                        );

        List<PtQuestionAnalysisResponse> result =
                new ArrayList<>();

        for (PtQuestionAnalysisResponse analysis : analyses) {

            /*
             * 현재 분석 결과의 questionNumber와
             * 실제 InterviewAnswer의 PT 질문 번호를 매칭한다.
             */
            InterviewAnswerEntity matchedAnswer =
                    answers.stream()
                            .filter(answer -> {

                                if (answer.getPtQuestion() == null) {
                                    return false;
                                }

                                Integer questionNumber =
                                        answer
                                                .getPtQuestion()
                                                .getQuestionNumber();

                                return questionNumber != null
                                        && questionNumber.equals(
                                        analysis.questionNumber()
                                );
                            })
                            .findFirst()
                            .orElse(null);

            /*
             * 실제 질문별 답변 STT
             *
             * FastAPI 분석 결과의 analysis.answer()가 아니라
             * interview_answers.answer_text를 사용한다.
             */
            String actualAnswer = "";

            if (
                    matchedAnswer != null
                            && matchedAnswer.getAnswerText() != null
            ) {
                actualAnswer =
                        matchedAnswer
                                .getAnswerText()
                                .trim();
            }

            /*
             * 질문별 Gemini 평가 조회
             */
            AiEvaluationEntity aiEvaluation = null;

            if (
                    matchedAnswer != null
                            && matchedAnswer.getId() != null
            ) {

                aiEvaluation =
                        aiEvaluationRepository
                                .findByInterviewAnswer_Id(
                                        matchedAnswer.getId()
                                )
                                .orElse(null);
            }

            /*
             * STAR 분석
             */
            PtStarAnalysisResponse starAnalysis =
                    aiEvaluation == null
                            ? null
                            : parseStarAnalysis(
                            aiEvaluation.getStarEvaluationJson()
                    );

            /*
             * Gemini 피드백
             */
            String aiFeedback =
                    aiEvaluation == null
                            ? null
                            : aiEvaluation.getGeminiFeedback();

            /*
             * AI 모범 답안
             */
            String modelAnswer =
                    aiEvaluation == null
                            ? null
                            : aiEvaluation.getModelAnswer();

            /*
             * 최종 질문별 분석 응답 생성
             */
            result.add(
                    new PtQuestionAnalysisResponse(
                            analysis.questionNumber(),
                            analysis.question(),

                            // 핵심 수정
                            actualAnswer,

                            analysis.understandingScore(),
                            analysis.logicScore(),
                            analysis.specificityScore(),
                            analysis.evidenceScore(),
                            analysis.overallScore(),
                            analysis.strengths(),
                            analysis.weaknesses(),
                            analysis.improvement(),
                            analysis.improvedAnswer(),
                            aiFeedback,
                            modelAnswer,
                            starAnalysis
                    )
            );
        }

        return result;
    }

    /**
     * DB에 저장된 STAR JSON:
     *
     * {
     *   "S": 6,
     *   "T": 7,
     *   "A": 5,
     *   "R": 5
     * }
     *
     * 를 FE가 사용하기 쉬운 DTO로 변환한다.
     */
    private PtStarAnalysisResponse parseStarAnalysis(
            String starJson
    ) {

        if (
                starJson == null
                        || starJson.isBlank()
        ) {
            return null;
        }

        try {

            JsonNode root =
                    objectMapper.readTree(
                            starJson
                    );

            Integer situation =
                    getIntegerValue(
                            root,
                            "S"
                    );

            Integer task =
                    getIntegerValue(
                            root,
                            "T"
                    );

            Integer action =
                    getIntegerValue(
                            root,
                            "A"
                    );

            Integer result =
                    getIntegerValue(
                            root,
                            "R"
                    );

            int totalScore =
                    safeInt(situation)
                            + safeInt(task)
                            + safeInt(action)
                            + safeInt(result);

            return new PtStarAnalysisResponse(
                    situation,
                    task,
                    action,
                    result,
                    totalScore
            );

        } catch (Exception e) {

            /*
             * STAR JSON 하나가 깨져 있어도
             * PT 리포트 전체 조회를 실패시키지 않는다.
             */
            return null;
        }
    }

    private Integer getIntegerValue(
            JsonNode root,
            String fieldName
    ) {

        if (
                root == null
                        || !root.has(fieldName)
                        || root.get(fieldName).isNull()
        ) {
            return null;
        }

        return root
                .get(fieldName)
                .asInt();
    }

    private int safeInt(
            Integer value
    ) {

        return value == null
                ? 0
                : value;
    }

    /**
     * Entity -> API Response
     */
    private PtReportResponse toResponse(
            PtReportEntity report
    ) {

        Long ptInterviewId =
                report
                        .getPtInterview()
                        .getId();

        if (
                report.getPtInterview().getInterview() == null
                        || report
                        .getPtInterview()
                        .getInterview()
                        .getId() == null
        ) {
            throw new IllegalStateException(
                    "PT 면접에 연결된 Interview 정보를 찾을 수 없습니다."
            );
        }

        Long interviewId =
                report
                        .getPtInterview()
                        .getInterview()
                        .getId();

        /*
         * PT FastAPI 질문 분석 결과 복원
         */
        List<PtQuestionAnalysisResponse> storedQuestionAnalyses =
                deserializeQuestionAnalyses(
                        report.getResultJson()
                );

        /*
         * 기존 AI 평가 데이터 결합
         */
        List<PtQuestionAnalysisResponse> enrichedQuestionAnalyses =
                enrichQuestionAnalyses(
                        ptInterviewId,
                        storedQuestionAnalyses
                );

        /*
         * 발표 상세 분석 구성
         */
        PtPresentationAnalysisResponse presentationAnalysis =
                buildPresentationAnalysis(
                        interviewId
                );

        return new PtReportResponse(
                report.getId(),
                ptInterviewId,
                report.getContentScore(),
                report.getStructureScore(),
                report.getDeliveryScore(),
                report.getQaScore(),
                report.getOverallScore(),
                report.getPresentationMaterialSummary(),
                report.getActualPresentationSummary(),
                report.getPresentationComparison(),
                presentationAnalysis,
                enrichedQuestionAnalyses,
                report.getStrengths(),
                report.getWeaknesses(),
                report.getImprovement(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
