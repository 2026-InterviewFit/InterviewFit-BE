package com.example.interviewfitbe.domain.pt_interview.controller;

import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentUploadResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtInterviewCreateRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtInterviewResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionResponse;
import com.example.interviewfitbe.domain.pt_interview.service.PtDocumentService;
import com.example.interviewfitbe.domain.pt_interview.service.PtInterviewService;
import com.example.interviewfitbe.domain.pt_interview.service.PtQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/pt-interviews")
@RequiredArgsConstructor
public class PtInterviewController {

    private final PtInterviewService ptInterviewService;
    private final PtDocumentService ptDocumentService;
    private final PtQuestionService ptQuestionService;

    /*
     * PT 면접 생성
     */
    @PostMapping
    public ResponseEntity<PtInterviewResponse> create(
            @RequestBody PtInterviewCreateRequest request
    ) {

        PtInterviewResponse response =
                ptInterviewService.create(
                        request
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * PT 발표자료 업로드
     *
     * 지원 형식:
     * PPTX / PDF
     */
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

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * PT 발표자료 기반 예상 질문 생성
     *
     * 흐름:
     *
     * PtDocument.summary
     * + PtSlide 분석 결과
     * → FastAPI
     * → Gemini
     * → 예상 질문 생성
     * → pt_question 저장
     */
    @PostMapping(
            "/{ptInterviewId}/questions/generate"
    )
    public ResponseEntity<List<PtQuestionResponse>> generateQuestions(
            @PathVariable Long ptInterviewId
    ) {

        List<PtQuestionResponse> response =
                ptQuestionService.generateQuestions(
                        ptInterviewId
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * 생성된 PT 예상 질문 조회
     */
    @GetMapping(
            "/{ptInterviewId}/questions"
    )
    public ResponseEntity<List<PtQuestionResponse>> getQuestions(
            @PathVariable Long ptInterviewId
    ) {

        List<PtQuestionResponse> response =
                ptQuestionService.getQuestions(
                        ptInterviewId
                );

        return ResponseEntity.ok(
                response
        );
    }
}