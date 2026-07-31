package com.example.interviewfitbe.domain.pt_interview.controller;

import com.example.interviewfitbe.domain.pt_interview.dto.PtAnswerResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtAnswerUploadResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentDetailResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentUploadResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtInterviewCreateRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtInterviewResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtReportResponse;
import com.example.interviewfitbe.domain.pt_interview.service.PtAnswerService;
import com.example.interviewfitbe.domain.pt_interview.service.PtDocumentService;
import com.example.interviewfitbe.domain.pt_interview.service.PtInterviewService;
import com.example.interviewfitbe.domain.pt_interview.service.PtQuestionService;
import com.example.interviewfitbe.domain.pt_interview.service.PtReportService;
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
    private final PtAnswerService ptAnswerService;
    private final PtReportService ptReportService;

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
     * PT 발표자료 분석 결과 조회
     */
    @GetMapping(
            "/{ptInterviewId}/document"
    )
    public ResponseEntity<PtDocumentDetailResponse> getDocumentDetail(
            @PathVariable Long ptInterviewId
    ) {

        PtDocumentDetailResponse response =
                ptDocumentService.getDocumentDetail(
                        ptInterviewId
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * PT 발표자료 기반 예상 질문 생성
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

    /*
     * PT 질문 답변 영상 업로드
     *
     * multipart/form-data:
     * ptQuestionId: 답변할 PT 질문 ID
     * file: 답변 영상
     */
    @PostMapping(
            value = "/{ptInterviewId}/answers",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<PtAnswerUploadResponse> uploadAnswer(
            @PathVariable Long ptInterviewId,
            @RequestPart("ptQuestionId") Long ptQuestionId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {

        PtAnswerUploadResponse response =
                ptAnswerService.uploadAnswer(
                        ptInterviewId,
                        ptQuestionId,
                        file
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * 특정 PT 면접의 답변 전체 조회
     */
    @GetMapping(
            "/{ptInterviewId}/answers"
    )
    public ResponseEntity<List<PtAnswerResponse>> getAnswers(
            @PathVariable Long ptInterviewId
    ) {

        List<PtAnswerResponse> response =
                ptAnswerService.getAnswers(
                        ptInterviewId
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * 특정 PT 질문의 답변 조회
     */
    @GetMapping(
            "/{ptInterviewId}/questions/{ptQuestionId}/answer"
    )
    public ResponseEntity<PtAnswerResponse> getAnswer(
            @PathVariable Long ptInterviewId,
            @PathVariable Long ptQuestionId
    ) {

        PtAnswerResponse response =
                ptAnswerService.getAnswer(
                        ptInterviewId,
                        ptQuestionId
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * PT 종합 리포트 생성
     *
     * 발표자료, 질문, 답변 및 분석 결과를 기반으로
     * FastAPI를 호출하여 종합 리포트를 생성한다.
     */
    @PostMapping(
            "/{ptInterviewId}/report/generate"
    )
    public ResponseEntity<PtReportResponse> generateReport(
            @PathVariable Long ptInterviewId
    ) {

        PtReportResponse response =
                ptReportService.generateReport(
                        ptInterviewId
                );

        return ResponseEntity.ok(
                response
        );
    }

    /*
     * 생성된 PT 종합 리포트 조회
     */
    @GetMapping(
            "/{ptInterviewId}/report"
    )
    public ResponseEntity<PtReportResponse> getReport(
            @PathVariable Long ptInterviewId
    ) {

        PtReportResponse response =
                ptReportService.getReport(
                        ptInterviewId
                );

        return ResponseEntity.ok(
                response
        );
    }
}