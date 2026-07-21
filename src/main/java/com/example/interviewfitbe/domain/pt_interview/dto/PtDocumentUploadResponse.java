package com.example.interviewfitbe.domain.pt_interview.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class PtDocumentUploadResponse {

    private Long documentId;

    private Long ptInterviewId;

    private String originalFileName;

    private String objectKey;

    private String analysisStatus;
}
