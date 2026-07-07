package com.example.interviewfitbe.domain.video_storage.service;

import com.example.interviewfitbe.domain.video_storage.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.video_storage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.video_storage.entity.VideoEntity;
import com.example.interviewfitbe.domain.video_storage.repository.VideoRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoService {

    private static final String DEFAULT_VIDEO_FILE_NAME = "interview.webm";
    private static final String DEFAULT_CONTENT_TYPE = "video/webm";
    private static final String VIDEO_DIRECTORY = "videos/";
    private static final String FASTAPI_SAVE_PATH = "/video_storage/save";

    private final MinioClient minioClient;
    private final VideoRepository videoRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    public VideoUrlResponse saveVideoFile(MultipartFile file) throws IOException {
        validateFile(file);

        String originalFileName = getOriginalFileName(file);
        String objectKey = uploadVideoToMinio(file, originalFileName);

        saveVideoMetadata(originalFileName, objectKey);
        requestFastApiSave(objectKey);

        return new VideoUrlResponse("Video file saved to MinIO and DB", objectKey);
    }

    public String uploadVideoToMinio(MultipartFile file) throws IOException {
        validateFile(file);

        String originalFileName = getOriginalFileName(file);
        return uploadVideoToMinio(file, originalFileName);
    }

    private String uploadVideoToMinio(MultipartFile file, String originalFileName) throws IOException {
        String objectKey = createObjectKey(originalFileName);

        try {
            createBucketIfNotExists();

            String contentType = normalizeContentType(file.getContentType());

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

            log.info("MinIO 업로드 성공: bucket={}, objectKey={}, contentType={}",
                    bucketName, objectKey, contentType);

            return objectKey;

        } catch (Exception e) {
            log.error("MinIO 업로드 실패: {}", e.getMessage(), e);
            throw new IOException("MinIO 업로드 실패: " + e.getMessage(), e);
        }
    }

    private void saveVideoMetadata(String originalFileName, String objectKey) {
        VideoEntity video = VideoEntity.builder()
                .originalFileName(originalFileName)
                .objectKey(objectKey)
                .build();

        videoRepository.save(video);
    }

    private void requestFastApiSave(String objectKey) {
        VideoUrlRequest request = new VideoUrlRequest(objectKey);
        String fastApiUrl = fastApiBaseUrl + FASTAPI_SAVE_PATH;

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(
                    fastApiUrl,
                    request,
                    String.class
            );

            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new IllegalStateException("FastAPI 서버 응답 실패: " + response.getStatusCode());
            }

            log.info("FastAPI 저장 요청 성공: {}", response.getBody());

        } catch (Exception e) {
            log.error("FastAPI 저장 요청 실패: {}", e.getMessage(), e);
        }
    }

    private void validateFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("업로드된 파일이 비어 있습니다.");
        }
    }

    private String getOriginalFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            return DEFAULT_VIDEO_FILE_NAME;
        }

        return originalFileName;
    }

    private String createObjectKey(String originalFileName) {
        return VIDEO_DIRECTORY + UUID.randomUUID() + "_" + originalFileName;
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return DEFAULT_CONTENT_TYPE;
        }

        if (contentType.contains(";")) {
            return contentType.split(";")[0].trim();
        }

        return contentType.trim();
    }

    private void createBucketIfNotExists() throws Exception {
        boolean exists = minioClient.bucketExists(
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

            log.info("MinIO bucket 생성 완료: {}", bucketName);
        }
    }
}