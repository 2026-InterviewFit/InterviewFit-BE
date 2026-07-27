package com.example.interviewfitbe.domain.pt_interview.dto;

import com.example.interviewfitbe.domain.pt_interview.entity.PtSlideEntity;

public record PtSlideResponse(

        Long slideId,

        Integer slideNumber,

        String title,

        String extractedText,

        String summary,

        String imageObjectKey,

        String analysisJson

) {

    public static PtSlideResponse from(
            PtSlideEntity slide
    ) {
        return new PtSlideResponse(
                slide.getId(),
                slide.getSlideNumber(),
                slide.getTitle(),
                slide.getExtractedText(),
                slide.getSummary(),
                slide.getImageObjectKey(),
                slide.getAnalysisJson()
        );
    }
}