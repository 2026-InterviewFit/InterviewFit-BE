package com.example.interviewfitbe.domain.pt_interview.dto;

import com.example.interviewfitbe.domain.pt_interview.entity.PtDocumentEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtSlideEntity;

import java.util.List;

public record PtDocumentDetailResponse(

        Long documentId,

        Long ptInterviewId,

        String originalFileName,

        String objectKey,

        String contentType,

        Long fileSize,

        String analysisStatus,

        Integer totalSlides,

        String summary,

        String errorMessage,

        List<PtSlideResponse> slides

) {

    public static PtDocumentDetailResponse from(
            PtDocumentEntity document,
            List<PtSlideEntity> slides
    ) {

        List<PtSlideResponse> slideResponses =
                slides.stream()
                        .map(PtSlideResponse::from)
                        .toList();

        return new PtDocumentDetailResponse(
                document.getId(),
                document.getPtInterview().getId(),
                document.getOriginalFileName(),
                document.getObjectKey(),
                document.getContentType(),
                document.getFileSize(),
                document.getAnalysisStatus().name(),
                document.getTotalSlides(),
                document.getSummary(),
                document.getErrorMessage(),
                slideResponses
        );
    }
}
