package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentAnalyzeResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtSlideAnalyzeResult;
import com.example.interviewfitbe.domain.pt_interview.entity.PtDocumentEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtSlideEntity;
import com.example.interviewfitbe.domain.pt_interview.repository.PtDocumentRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtSlideRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PtDocumentAnalysisJobService {

    private final PtDocumentRepository ptDocumentRepository;
    private final PtSlideRepository ptSlideRepository;
    private final PtDocumentAnalysisService ptDocumentAnalysisService;

    @Transactional
    public void analyze(Long documentId) {

        PtDocumentEntity document = ptDocumentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "PT 문서를 찾을 수 없습니다. documentId=" + documentId
                ));

        document.startAnalysis();

        try {
            PtDocumentAnalyzeResponse response =
                    ptDocumentAnalysisService.analyzeDocument(
                            document.getObjectKey()
                    );

            validateResponse(response);

            ptSlideRepository.deleteAllByDocumentId(
                    document.getId()
            );

            for (PtSlideAnalyzeResult result : response.slides()) {

                PtSlideEntity slide = PtSlideEntity.create(
                        document,
                        result.slideNumber(),
                        result.title(),
                        result.extractedText(),
                        result.summary()
                );

                ptSlideRepository.save(slide);
            }

            document.completeAnalysis(
                    response.summary(),
                    response.totalSlides()
            );

        } catch (Exception e) {
            document.failAnalysis(getErrorMessage(e));
        }
    }

    private void validateResponse(
            PtDocumentAnalyzeResponse response
    ) {
        if (response == null) {
            throw new IllegalStateException(
                    "FastAPI 문서 분석 응답이 없습니다."
            );
        }

        if (response.slides() == null) {
            throw new IllegalStateException(
                    "슬라이드 분석 결과가 없습니다."
            );
        }
    }

    private String getErrorMessage(Exception e) {

        if (e.getMessage() == null ||
                e.getMessage().isBlank()) {
            return e.getClass().getSimpleName();
        }

        return e.getMessage();
    }
}