package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentAnalyzeResponse;
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

    /*
     * FastAPI PT 문서 분석 호출 서비스
     */
    private final PtDocumentAnalysisService ptDocumentAnalysisService;

    @Value("${minio.bucket}")
    private String bucketName;

    /*
     * PT 발표자료 업로드
     *
     * 흐름:
     *
     * 1. 파일 검증
     * 2. PT 면접 조회
     * 3. MinIO 업로드
     * 4. PtDocument DB 저장
     * 5. FastAPI 분석 요청
     * 6. 슬라이드별 분석 결과 저장
     * 7. 분석 상태 COMPLETED / FAILED 변경
     */
    @Transactional
    public PtDocumentUploadResponse uploadDocument(
            Long ptInterviewId,
            MultipartFile file
    ) throws IOException {

        /*
         * 1. 파일 검증
         */
        validateFile(file);

        /*
         * 2. PT 면접 조회
         */
        PtInterviewEntity ptInterview =
                ptInterviewRepository
                        .findById(ptInterviewId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "존재하지 않는 PT 면접입니다."
                                )
                        );

        /*
         * 하나의 PT 면접에는 하나의 발표자료만 허용
         */
        if (
                ptDocumentRepository
                        .findByPtInterviewId(ptInterviewId)
                        .isPresent()
        ) {
            throw new IllegalStateException(
                    "이미 업로드된 PT 발표자료가 존재합니다."
            );
        }

        String originalFileName =
                getOriginalFileName(file);

        /*
         * 3. MinIO 업로드
         */
        String objectKey =
                uploadToMinio(
                        ptInterviewId,
                        file,
                        originalFileName
                );

        /*
         * 4. PtDocument 생성
         */
        PtDocumentEntity document =
                PtDocumentEntity.create(
                        ptInterview,
                        originalFileName,
                        objectKey,
                        file.getContentType(),
                        file.getSize()
                );

        PtDocumentEntity saved =
                ptDocumentRepository.save(document);

        /*
         * 5. FastAPI 분석 시작
         */
        analyzeAndSaveSlides(saved);

        /*
         * 최종 상태를 포함해서 반환
         */
        return new PtDocumentUploadResponse(
                saved.getId(),
                ptInterview.getId(),
                saved.getOriginalFileName(),
                saved.getObjectKey(),
                saved.getAnalysisStatus().name()
        );
    }

    /*
     * FastAPI 문서 분석
     *
     * MinIO objectKey를 FastAPI에 전달하고
     * 분석된 슬라이드를 PtSlide 테이블에 저장한다.
     */
    private void analyzeAndSaveSlides(
            PtDocumentEntity document
    ) {

        /*
         * UPLOADED → ANALYZING
         */
        document.startAnalysis();

        try {

            /*
             * FastAPI
             *
             * POST /apis/pt-document/analyze
             */
            PtDocumentAnalyzeResponse response =
                    ptDocumentAnalysisService
                            .analyzeDocument(
                                    document.getObjectKey()
                            );

            /*
             * 기존 슬라이드 데이터가 있다면 삭제
             *
             * 현재는 최초 업로드만 허용하지만
             * 추후 재분석 기능을 고려한 처리
             */
            ptSlideRepository.deleteAllByDocumentId(
                    document.getId()
            );

            /*
             * FastAPI에서 받은 슬라이드별 분석 결과 저장
             */
            if (
                    response != null
                            && response.slides() != null
            ) {

                for (
                        PtSlideAnalyzeResult slide
                        : response.slides()
                ) {

                    PtSlideEntity slideEntity =
                            PtSlideEntity.create(
                                    document,
                                    slide.slideNumber(),
                                    slide.title(),
                                    slide.extractedText(),
                                    slide.summary(),

                                    /*
                                     * 이미지 변환 기능은 아직 미구현
                                     */
                                    null,

                                    /*
                                     * 추가 AI 분석 JSON도 아직 미구현
                                     */
                                    null
                            );

                    ptSlideRepository.save(
                            slideEntity
                    );
                }
            }

            /*
             * 현재 FastAPI에서는 문서 전체 summary를
             * 반환하지 않으므로 null 처리
             *
             * 추후 Gemini 전체 요약 연결 예정
             */
            document.completeAnalysis(
                    response.summary()
            );

            log.info(
                    "PT 발표자료 분석 완료: documentId={}, objectKey={}, totalSlides={}",
                    document.getId(),
                    document.getObjectKey(),
                    response != null
                            ? response.totalSlides()
                            : 0
            );

        } catch (Exception e) {

            /*
             * 분석 실패 상태 저장
             *
             * 여기서는 예외를 다시 던지지 않는다.
             * 그래야 PtDocument의 FAILED 상태가
             * 트랜잭션 롤백으로 사라지지 않는다.
             */
            document.failAnalysis(
                    e.getMessage()
            );

            log.error(
                    "PT 발표자료 분석 실패: documentId={}, objectKey={}, error={}",
                    document.getId(),
                    document.getObjectKey(),
                    e.getMessage(),
                    e
            );
        }
    }

    /*
     * MinIO 파일 업로드
     */
    private String uploadToMinio(
            Long ptInterviewId,
            MultipartFile file,
            String originalFileName
    ) throws IOException {

        String objectKey =
                createObjectKey(
                        ptInterviewId,
                        originalFileName
                );

        try {

            createBucketIfNotExists();

            String contentType =
                    normalizeContentType(
                            file.getContentType()
                    );

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
                    "PT 발표자료 MinIO 업로드 실패: {}",
                    e.getMessage(),
                    e
            );

            throw new IOException(
                    "PT 발표자료 MinIO 업로드 실패: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /*
     * 업로드 파일 검증
     *
     * 현재 FastAPI 분석 서비스가 지원하는 형식:
     *
     * - PPTX
     * - PDF
     *
     * 구형 PPT(.ppt)는 지원하지 않음
     */
    private void validateFile(
            MultipartFile file
    ) throws IOException {

        if (
                file == null
                        || file.isEmpty()
        ) {

            throw new IOException(
                    "업로드된 파일이 비어 있습니다."
            );
        }

        String fileName =
                file.getOriginalFilename();

        if (
                fileName == null
                        || fileName.isBlank()
        ) {

            throw new IOException(
                    "파일명이 존재하지 않습니다."
            );
        }

        String lowerFileName =
                fileName.toLowerCase();

        if (
                !lowerFileName.endsWith(".pptx")
                        && !lowerFileName.endsWith(".pdf")
        ) {

            throw new IOException(
                    "PPTX, PDF 파일만 업로드할 수 있습니다."
            );
        }
    }

    /*
     * 원본 파일명 반환
     */
    private String getOriginalFileName(
            MultipartFile file
    ) {

        return file.getOriginalFilename();
    }

    /*
     * MinIO objectKey 생성
     *
     * 예:
     *
     * pt-documents/
     *   15/
     *     uuid_presentation.pptx
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

        if (
                contentType == null
                        || contentType.isBlank()
        ) {

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
     * MinIO bucket 존재 여부 확인
     *
     * 없으면 자동 생성
     */
    private void createBucketIfNotExists()
            throws Exception {

        boolean exists =
                minioClient.bucketExists(
                        BucketExistsArgs
                                .builder()
                                .bucket(bucketName)
                                .build()
                );

        if (!exists) {

            minioClient.makeBucket(
                    MakeBucketArgs
                            .builder()
                            .bucket(bucketName)
                            .build()
            );

            log.info(
                    "MinIO bucket 생성 완료: {}",
                    bucketName
            );
        }
    }
}