package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.pt_interview.dto.PtDocumentUploadResponse;
import com.example.interviewfitbe.domain.pt_interview.entity.PtDocumentEntity;
import com.example.interviewfitbe.domain.pt_interview.entity.PtInterviewEntity;
import com.example.interviewfitbe.domain.pt_interview.repository.PtDocumentRepository;
import com.example.interviewfitbe.domain.pt_interview.repository.PtInterviewRepository;
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

    @Value("${minio.bucket}")
    private String bucketName;

    @Transactional
    public PtDocumentUploadResponse uploadDocument(
            Long ptInterviewId,
            MultipartFile file
    ) throws IOException {

        validateFile(file);

        PtInterviewEntity ptInterview =
                ptInterviewRepository
                        .findById(ptInterviewId)
                        .orElseThrow(
                                () -> new IllegalArgumentException(
                                        "존재하지 않는 PT 면접입니다."
                                )
                        );

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

        String objectKey =
                uploadToMinio(
                        ptInterviewId,
                        file,
                        originalFileName
                );

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

        return new PtDocumentUploadResponse(
                saved.getId(),
                ptInterview.getId(),
                saved.getOriginalFileName(),
                saved.getObjectKey(),
                saved.getAnalysisStatus().name()
        );
    }

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
                !lowerFileName.endsWith(".ppt")
                        && !lowerFileName.endsWith(".pptx")
                        && !lowerFileName.endsWith(".pdf")
        ) {
            throw new IOException(
                    "PPT, PPTX, PDF 파일만 업로드할 수 있습니다."
            );
        }
    }

    private String getOriginalFileName(
            MultipartFile file
    ) {

        return file.getOriginalFilename();
    }

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