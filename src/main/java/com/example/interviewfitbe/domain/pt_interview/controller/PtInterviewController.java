package com.example.interviewfitbe.domain.pt_interview.controller;

import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentUploadResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtInterviewCreateRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtInterviewResponse;
import com.example.interviewfitbe.domain.pt_interview.service.PtDocumentService;
import com.example.interviewfitbe.domain.pt_interview.service.PtInterviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/pt-interviews")
@RequiredArgsConstructor
public class PtInterviewController {

    private final PtInterviewService ptInterviewService;
    private final PtDocumentService ptDocumentService;

    @PostMapping
    public ResponseEntity<PtInterviewResponse> create(
            @RequestBody PtInterviewCreateRequest request
    ) {
        PtInterviewResponse response =
                ptInterviewService.create(request);

        return ResponseEntity.ok(response);
    }

    @PostMapping(
            value = "/{ptInterviewId}/document",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PtDocumentUploadResponse> uploadDocument(
            @PathVariable Long ptInterviewId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {

        PtDocumentUploadResponse response =
                ptDocumentService.uploadDocument(
                        ptInterviewId,
                        file
                );

        return ResponseEntity.ok(response);
    }
}