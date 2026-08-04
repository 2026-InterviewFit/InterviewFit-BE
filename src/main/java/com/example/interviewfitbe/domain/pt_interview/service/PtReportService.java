package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import com.example.interviewfitbe.domain.eye_gaze.repository.EyeGazeReportRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import com.example.interviewfitbe.domain.pose.repository.PoseReportRepository;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


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

    @Transactional
    public PtReportResponse generateReport(Long ptInterviewId) {

        PtInterviewEntity ptInterview =
                ptInterviewRepository
                        .findById(ptInterviewId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 PT 면접입니다."
                                )
                        );

        PtDocumentEntity document =
                ptDocumentRepository
                        .findByPtInterviewId(ptInterviewId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "PT 발표 자료를 찾을 수 없습니다."
                                )
                        );

        List<PtQuestionEntity> questions =
                ptQuestionRepository
                        .findAllByPtInterviewIdOrderByQuestionNumberAsc(
                                ptInterviewId
                        );

        List<InterviewAnswerEntity> answers =
                interviewAnswerRepository
                        .findAllByPtQuestion_PtInterview_IdOrderByAnswerOrderAsc(
                                ptInterviewId
                        );

        String presentationSummary =
                document.getSummary() == null
                        ? ""
                        : document.getSummary();

        Long interviewId =
                ptInterview.getInterview().getId();

        List<Double> eyeScores =
                extractEyeScores(interviewId);

        List<Double> blinkScores =
                extractBlinkScores(answers);

        List<Double> poseScores =
                extractPoseScores(interviewId);

        /*
         * SpeechReportEntity에 종합 점수 필드가 아직 없으므로
         * 현재는 빈 배열로 FastAPI에 전달한다.
         */
        List<Double> speechScores =
                speechReportRepository
                        .findByInterview_Id(interviewId)
                        .map(report ->
                                List.of(
                                        report.getScore() == null
                                                ? 0.0
                                                : report.getScore()
                                )
                        )
                        .orElseGet(List::of);

        PtReportGenerateRequest request =
                new PtReportGenerateRequest(
                        presentationSummary,
                        extractQuestions(questions),
                        extractAnswers(answers),
                        eyeScores,
                        blinkScores,
                        poseScores,
                        speechScores
                );

        PtReportGenerateResponse response =
                ptReportAnalysisService.generateReport(request);

        PtReportEntity report =
                ptReportRepository
                        .findByPtInterviewId(ptInterviewId)
                        .map(existingReport -> {
                            existingReport.update(
                                    response.contentScore(),
                                    response.structureScore(),
                                    response.deliveryScore(),
                                    response.qaScore(),
                                    response.overallScore(),
                                    response.presentationSummary(),
                                    response.strengths(),
                                    response.weaknesses(),
                                    response.improvement(),
                                    null
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
                                        response.presentationSummary(),
                                        response.strengths(),
                                        response.weaknesses(),
                                        response.improvement(),
                                        null
                                )
                        );

        PtReportEntity saved =
                ptReportRepository.save(report);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PtReportResponse getReport(Long ptInterviewId) {

        PtReportEntity report =
                ptReportRepository
                        .findByPtInterviewId(ptInterviewId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "PT 리포트가 존재하지 않습니다."
                                )
                        );

        return toResponse(report);
    }

    private List<String> extractQuestions(
            List<PtQuestionEntity> questions
    ) {
        return questions.stream()
                .map(question ->
                        question.getQuestion() == null
                                ? ""
                                : question.getQuestion()
                )
                .toList();
    }

    private List<String> extractAnswers(
            List<InterviewAnswerEntity> answers
    ) {
        return answers.stream()
                .map(answer ->
                        answer.getAnswerText() == null
                                ? ""
                                : answer.getAnswerText()
                )
                .toList();
    }

    /*
     * 면접 기준 가장 최근 시선 분석 점수 조회
     */
    private List<Double> extractEyeScores(
            Long interviewId
    ) {
        return eyeGazeReportRepository
                .findTopByInterviewIdOrderByCreatedAtDesc(interviewId)
                .map(report -> {
                    Double score =
                            report.getScore() == null
                                    ? 0.0
                                    : report.getScore();

                    return List.of(score);
                })
                .orElseGet(List::of);
    }

    /*
     * 각 PT 답변별 가장 최근 눈 깜빡임 점수 조회
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
                                .orElse(0.0)
                )
                .toList();
    }

    /*
     * 면접 기준 가장 최근 자세 분석 점수 조회
     */
    private List<Double> extractPoseScores(
            Long interviewId
    ) {
        return poseReportRepository
                .findTopByInterview_IdOrderByIdDesc(interviewId)
                .map(report -> {
                    Double score =
                            report.getScore() == null
                                    ? 0.0
                                    : report.getScore().doubleValue();

                    return List.of(score);
                })
                .orElseGet(List::of);
    }

    private PtReportResponse toResponse(
            PtReportEntity report
    ) {
        return new PtReportResponse(
                report.getId(),
                report.getPtInterview().getId(),
                report.getContentScore(),
                report.getStructureScore(),
                report.getDeliveryScore(),
                report.getQaScore(),
                report.getOverallScore(),
                report.getPresentationSummary(),
                report.getStrengths(),
                report.getWeaknesses(),
                report.getImprovement(),
                report.getCreatedAt(),
                report.getUpdatedAt()
        );
    }
}
