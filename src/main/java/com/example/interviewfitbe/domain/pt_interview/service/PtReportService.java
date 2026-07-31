package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
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

    @Transactional
    public PtReportResponse generateReport(
            Long ptInterviewId
    ) {

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

        PtReportGenerateRequest request =
                new PtReportGenerateRequest(
                        document.getSummary(),
                        extractQuestions(questions),
                        extractAnswers(answers),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>(),
                        new ArrayList<>()
                );

        PtReportGenerateResponse response =
                ptReportAnalysisService
                        .generateReport(request);

        PtReportEntity report =
                ptReportRepository
                        .findByPtInterviewId(ptInterviewId)
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

        report.update(
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

        PtReportEntity saved =
                ptReportRepository.save(report);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public PtReportResponse getReport(
            Long ptInterviewId
    ) {

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
                .map(PtQuestionEntity::getQuestion)
                .toList();
    }

    private List<String> extractAnswers(
            List<InterviewAnswerEntity> answers
    ) {

        return answers.stream()
                .map(answer -> {

                    if (answer.getAnswerText() == null) {
                        return "";
                    }

                    return answer.getAnswerText();
                })
                .toList();
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
