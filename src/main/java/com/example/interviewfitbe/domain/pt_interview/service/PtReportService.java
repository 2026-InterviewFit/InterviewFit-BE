package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import com.example.interviewfitbe.domain.eye_gaze.repository.EyeGazeReportRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.pose.repository.PoseReportRepository;
import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionAnalysisResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionAnswerRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtReportGenerateRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtReportGenerateResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtReportResponse;
import com.example.interviewfitbe.domain.pt_interview.entity.PtDocumentEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtInterviewEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtQuestionEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtReportEntity;
import com.example.interviewfitbe.domain.pt_interview.repository.PtDocumentRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtInterviewRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtQuestionRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtReportRepository;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import com.fasterxml.jackson.core.type.TypeReference;
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
         *
         * textScript:
         * 사용자가 실제 발표에서 말한 STT
         *
         * score:
         * 음성 전달력 분석 점수
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
                                List.of(
                                        report.getScore() == null
                                                ? 0.0
                                                : report.getScore()
                                )
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
             * 기존 데이터가 다른 형식일 수 있으므로
             * 조회 자체가 실패하지 않도록 빈 배열 반환
             */
            return List.of();
        }
    }

    /**
     * Entity -> API Response
     */
    private PtReportResponse toResponse(
            PtReportEntity report
    ) {

        List<PtQuestionAnalysisResponse> questionAnalyses =
                deserializeQuestionAnalyses(
                        report.getResultJson()
                );

        return new PtReportResponse(
                report.getId(),
                report.getPtInterview().getId(),
                report.getContentScore(),
                report.getStructureScore(),
                report.getDeliveryScore(),
                report.getQaScore(),
                report.getOverallScore(),
                report.getPresentationMaterialSummary(),
                report.getActualPresentationSummary(),
                report.getPresentationComparison(),
                questionAnalyses,
                report.getStrengths(),
                report.getWeaknesses(),
                report.getImprovement(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}