package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentAnalyzeResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentDetailResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentUploadResponse;
import com.example.interviewfitbe.domain.pt_interview.dto.PtSlideAnalyzeResult;
import com.example.interviewfitbe.domain.pt_interview.entity.PtDocumentEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtInterviewEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtSlideEntity;
import com.example.interviewfitbe.domain.pt_interview.repository.PtDocumentRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtInterviewRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtSlideRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PtDocumentService {

    private static final String DOCUMENT_DIRECTORY = "pt-documents/";

    private final MinioClient minioClient;
    private final PtInterviewRepository ptInterviewRepository;
    private final PtDocumentRepository ptDocumentRepository;
    private final PtSlideRepository ptSlideRepository;
    private final PtDocumentAnalysisService ptDocumentAnalysisService;

    @Value("${minio.bucket}")
    private String bucketName;

    /*
     * PT 발표자료 업로드
     *
     * 1. 파일 검증
     * 2. PT 면접 조회
     * 3. 중복 업로드 확인
     * 4. MinIO 업로드
     * 5. PtDocument 저장
     * 6. FastAPI 문서 분석
     * 7. PtSlide 저장
     * 8. 분석 상태 변경
     */
    @Transactional
    public PtDocumentUploadResponse uploadDocument(
            Long ptInterviewId,
            MultipartFile file
    ) throws IOException {

        validateFile(file);

        PtInterviewEntity ptInterview =
                ptInterviewRepository.findById(ptInterviewId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "존재하지 않는 PT 면접입니다. ptInterviewId="
                                                + ptInterviewId
                                )
                        );

        if (ptDocumentRepository
                .findByPtInterviewId(ptInterviewId)
                .isPresent()) {

            throw new IllegalStateException(
                    "이미 업로드된 PT 발표자료가 존재합니다."
            );
        }

        String originalFileName =
                getOriginalFileName(file);

        String contentType =
                normalizeContentType(
                        file.getContentType()
                );

        String objectKey =
                uploadToMinio(
                        ptInterviewId,
                        file,
                        originalFileName,
                        contentType
                );

        PtDocumentEntity document =
                PtDocumentEntity.create(
                        ptInterview,
                        originalFileName,
                        objectKey,
                        contentType,
                        file.getSize()
                );

        PtDocumentEntity savedDocument =
                ptDocumentRepository.save(document);

        analyzeAndSaveSlides(savedDocument);

        return new PtDocumentUploadResponse(
                savedDocument.getId(),
                ptInterview.getId(),
                savedDocument.getOriginalFileName(),
                savedDocument.getObjectKey(),
                savedDocument.getAnalysisStatus().name()
        );
    }

    /*
     * PT 발표자료 분석 결과 조회
     *
     * 문서 정보와 슬라이드별 분석 결과를 함께 반환한다.
     */
    @Transactional(readOnly = true)
    public PtDocumentDetailResponse getDocumentDetail(
            Long ptInterviewId
    ) {

        /*
         * PT 면접 존재 여부 확인
         */
        if (!ptInterviewRepository.existsById(ptInterviewId)) {
            throw new IllegalArgumentException(
                    "존재하지 않는 PT 면접입니다. ptInterviewId="
                            + ptInterviewId
            );
        }

        /*
         * PT 면접에 연결된 발표자료 조회
         */
        PtDocumentEntity document =
                ptDocumentRepository
                        .findByPtInterviewId(ptInterviewId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "업로드된 PT 발표자료가 없습니다. ptInterviewId="
                                                + ptInterviewId
                                )
                        );

        /*
         * 슬라이드 번호 순서대로 조회
         */
        List<PtSlideEntity> slides =
                ptSlideRepository
                        .findAllByDocumentIdOrderBySlideNumberAsc(
                                document.getId()
                        );

        return PtDocumentDetailResponse.from(
                document,
                slides
        );
    }

    /*
     * FastAPI 문서 분석 및 슬라이드 저장
     */
    private void analyzeAndSaveSlides(
            PtDocumentEntity document
    ) {

        document.startAnalysis();

        try {
            PtDocumentAnalyzeResponse response =
                    ptDocumentAnalysisService.analyzeDocument(
                            document.getObjectKey()
                    );

            validateAnalysisResponse(response);

            ptSlideRepository.deleteAllByDocumentId(
                    document.getId()
            );

            for (PtSlideAnalyzeResult slide : response.slides()) {

                PtSlideEntity slideEntity =
                        PtSlideEntity.create(
                                document,
                                slide.slideNumber(),
                                slide.title(),
                                slide.extractedText(),
                                slide.summary()
                        );

                ptSlideRepository.save(slideEntity);
            }

            document.completeAnalysis(
                    response.summary(),
                    response.totalSlides()
            );

            log.info(
                    "PT 발표자료 분석 완료: documentId={}, objectKey={}, totalSlides={}",
                    document.getId(),
                    document.getObjectKey(),
                    response.totalSlides()
            );

        } catch (Exception e) {

            String errorMessage =
                    getErrorMessage(e);

            document.failAnalysis(errorMessage);

            log.error(
                    "PT 발표자료 분석 실패: documentId={}, objectKey={}, error={}",
                    document.getId(),
                    document.getObjectKey(),
                    errorMessage,
                    e
            );
        }
    }

    /*
     * FastAPI 응답 검증
     */
    private void validateAnalysisResponse(
            PtDocumentAnalyzeResponse response
    ) {

        if (response == null) {
            throw new IllegalStateException(
                    "FastAPI 문서 분석 응답이 없습니다."
            );
        }

        if (response.slides() == null) {
            throw new IllegalStateException(
                    "FastAPI 슬라이드 분석 결과가 없습니다."
            );
        }

        if (response.totalSlides() == null) {
            throw new IllegalStateException(
                    "FastAPI 전체 슬라이드 수가 없습니다."
            );
        }
    }

    /*
     * MinIO 파일 업로드
     */
    private String uploadToMinio(
            Long ptInterviewId,
            MultipartFile file,
            String originalFileName,
            String contentType
    ) throws IOException {

        String objectKey =
                createObjectKey(
                        ptInterviewId,
                        originalFileName
                );

        try {
            createBucketIfNotExists();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(
                                    file.getInputStream(),
                                    file.getSize(),
                                    -1
                            )
                            .contentType(contentType)
                            .build()
            );

            log.info(
                    "PT 발표자료 MinIO 업로드 성공: bucket={}, objectKey={}",
                    bucketName,
                    objectKey
            );

            return objectKey;

        } catch (Exception e) {

            log.error(
                    "PT 발표자료 MinIO 업로드 실패: ptInterviewId={}, error={}",
                    ptInterviewId,
                    e.getMessage(),
                    e
            );

            throw new IOException(
                    "PT 발표자료 MinIO 업로드 실패: "
                            + getErrorMessage(e),
                    e
            );
        }
    }

    /*
     * 업로드 파일 검증
     *
     * 지원 형식:
     * - PPTX
     * - PDF
     */
    private void validateFile(
            MultipartFile file
    ) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IOException(
                    "업로드된 파일이 비어 있습니다."
            );
        }

        String fileName =
                file.getOriginalFilename();

        if (fileName == null || fileName.isBlank()) {
            throw new IOException(
                    "파일명이 존재하지 않습니다."
            );
        }

        String lowerFileName =
                fileName.toLowerCase();

        if (!lowerFileName.endsWith(".pptx")
                && !lowerFileName.endsWith(".pdf")) {

            throw new IOException(
                    "PPTX 또는 PDF 파일만 업로드할 수 있습니다."
            );
        }
    }

    /*
     * 원본 파일명 반환
     */
    private String getOriginalFileName(
            MultipartFile file
    ) throws IOException {

        String originalFileName =
                file.getOriginalFilename();

        if (originalFileName == null
                || originalFileName.isBlank()) {

            throw new IOException(
                    "파일명이 존재하지 않습니다."
            );
        }

        return originalFileName
                .replace("\\", "_")
                .replace("/", "_");
    }

    /*
     * MinIO ObjectKey 생성
     *
     * 예:
     * pt-documents/15/uuid_presentation.pptx
     */
    private String createObjectKey(
            Long ptInterviewId,
            String originalFileName
    ) {

        return DOCUMENT_DIRECTORY
                + ptInterviewId
                + "/"
                + UUID.randomUUID()
                + "_"
                + originalFileName;
    }

    /*
     * Content-Type 정리
     */
    private String normalizeContentType(
            String contentType
    ) {

        if (contentType == null
                || contentType.isBlank()) {

            return "application/octet-stream";
        }

        if (contentType.contains(";")) {
            return contentType
                    .split(";")[0]
                    .trim();
        }

        return contentType.trim();
    }

    /*
     * MinIO Bucket 확인 및 생성
     */
    private void createBucketIfNotExists()
            throws Exception {

        boolean exists =
                minioClient.bucketExists(
                        BucketExistsArgs.builder()
                                .bucket(bucketName)
                                .build()
                );

        if (!exists) {
            minioClient.makeBucket(
                    MakeBucketArgs.builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info(
                    "MinIO bucket 생성 완료: {}",
                    bucketName
            );
        }
    }

    private String getErrorMessage(
            Exception e
    ) {

        String message =
                e.getMessage();

        if (message == null || message.isBlank()) {
            return e.getClass().getSimpleName();
        }

        return message;
    }
}