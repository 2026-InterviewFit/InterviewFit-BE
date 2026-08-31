package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record PtDocumentAnalyzeResponse(

        @JsonProperty("object_key")
        String objectKey,

        @JsonProperty("file_type")
        String fileType,

        @JsonProperty("total_slides")
        Integer totalSlides,

        String summary,

        List<PtSlideAnalyzeResult> slides

) {
}