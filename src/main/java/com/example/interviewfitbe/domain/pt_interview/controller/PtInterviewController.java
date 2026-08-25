package com.example.interviewfitbe.domain.pt_interview.controller;

import com.example.interviewfitbe.domain.pt_interview.dto.PtAnswerResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtAnswerUploadResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentDetailResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentUploadResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtInterviewCreateRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtInterviewResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtReportResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtSlideResponse;
import com.example.interviewfitbe.domain.pt_interview.service.*;
import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import io.minio.StatObjectArgs;
import io.minio.StatObjectResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
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
    private final PtPresentationService ptPresentationService;

    /*
     * MinIO
     */
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

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
     * PT 슬라이드 이미지 조회
     *
     * DA에서 PDF 페이지를 PNG로 변환해
     * MinIO에 저장한 이미지를 브라우저에 반환한다.
     */
    @GetMapping(
            "/{ptInterviewId}/slides/{slideNumber}/image"
    )
    public ResponseEntity<StreamingResponseBody> getSlideImage(
            @PathVariable Long ptInterviewId,
            @PathVariable Integer slideNumber
    ) {

        PtDocumentDetailResponse document =
                ptDocumentService.getDocumentDetail(
                        ptInterviewId
                );

        PtSlideResponse slide =
                document.slides()
                        .stream()
                        .filter(item ->
                                item.slideNumber() != null
                                        && item.slideNumber().equals(slideNumber)
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 PT 슬라이드입니다. slideNumber="
                                                + slideNumber
                                )
                        );

        String objectKey =
                slide.imageObjectKey();

        if (
                objectKey == null
                        || objectKey.isBlank()
        ) {

            return ResponseEntity
                    .notFound()
                    .build();
        }

        try {

            StatObjectResponse stat =
                    minioClient.statObject(
                            StatObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectKey)
                                    .build()
                    );

            StreamingResponseBody responseBody =
                    outputStream -> {

                        try (
                                InputStream inputStream =
                                        minioClient.getObject(
                                                GetObjectArgs.builder()
                                                        .bucket(bucketName)
                                                        .object(objectKey)
                                                        .build()
                                        )
                        ) {

                            inputStream.transferTo(
                                    outputStream
                            );

                        } catch (Exception e) {

                            throw new IOException(
                                    "PT 슬라이드 이미지 스트리밍 실패",
                                    e
                            );
                        }
                    };

            HttpHeaders headers =
                    new HttpHeaders();

            /*
             * 현재 DA에서 PDF 페이지를 PNG로 저장하므로
             * PNG 타입으로 반환.
             */
            headers.setContentType(
                    MediaType.IMAGE_PNG
            );

            headers.setContentLength(
                    stat.size()
            );

            headers.set(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "inline"
            );

            return new ResponseEntity<>(
                    responseBody,
                    headers,
                    HttpStatus.OK
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "PT 슬라이드 이미지 조회 실패: "
                            + e.getMessage(),
                    e
            );
        }
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
            @RequestParam("ptQuestionId") Long ptQuestionId,
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

    /*
     * PT 발표 전체 영상 업로드
     *
     * 발표 종료 시 FE에서 녹화된 전체 영상을 전달한다.
     * 업로드 후 Speech/STT 분석을 실행한다.
     */
    @PostMapping(
            value = "/{ptInterviewId}/presentation",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<Long> uploadPresentation(
            @PathVariable Long ptInterviewId,
            @RequestPart("file") MultipartFile file
    ) throws IOException {

        Long speechReportId =
                ptPresentationService.uploadAndAnalyze(
                        ptInterviewId,
                        file
                );

        return ResponseEntity.ok(
                speechReportId
        );
    }
}