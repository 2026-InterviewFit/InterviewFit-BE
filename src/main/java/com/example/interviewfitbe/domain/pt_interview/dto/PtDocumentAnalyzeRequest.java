package com.example.interviewfitbe.domain.pt_interview.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PtDocumentAnalyzeRequest(

        @JsonProperty("object_key")
        String objectKey

) {
}
