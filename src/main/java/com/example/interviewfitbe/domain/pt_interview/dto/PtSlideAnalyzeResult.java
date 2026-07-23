package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PtSlideAnalyzeResult(

        @JsonProperty("slide_number")
        Integer slideNumber,

        String title,

        @JsonProperty("extracted_text")
        String extractedText,

        String summary

) {
}