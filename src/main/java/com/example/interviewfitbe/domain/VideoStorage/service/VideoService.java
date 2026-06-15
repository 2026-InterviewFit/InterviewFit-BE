package com.example.interviewfitbe.domain.VideoStorage.service;

import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.VideoStorage.entity.VideoEntity;
import com.example.interviewfitbe.domain.VideoStorage.repository.VideoRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
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
public class VideoService {

    private final MinioClient minioClient;
    private final VideoRepository videoRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    public VideoService(
            MinioClient minioClient,
            VideoRepository videoRepository
    ) {
        this.minioClient = minioClient;
        this.videoRepository = videoRepository;
    }

    /**
     * 기존 /videos/upload API에서 사용하는 메서드.
     *
     * 흐름:
     * 1. MinIO 저장
     * 2. video 테이블 저장
     * 3. FastAPI /VideoStorage/save 호출
     *
     * 질문별 업로드 API에서는 이 메서드를 쓰지 않고,
     * uploadVideoToMinio()만 사용한다.
     */
    public VideoUrlResponse saveVideoFile(MultipartFile file) throws IOException {
        String objectKey = uploadVideoToMinio(file);

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "interview.webm";
        }

        VideoEntity video = new VideoEntity();
        video.setOriginalFileName(originalFileName);
        video.setObjectKey(objectKey);
        videoRepository.save(video);

        VideoUrlRequest request = new VideoUrlRequest();
        request.setVideoUrl(objectKey);

        try {
            sendToFastApi(request);
        } catch (Exception e) {
            log.error("FastAPI 분석 요청 실패: {}", e.getMessage(), e);
        }

        return new VideoUrlResponse(
                "Video file saved to MinIO and DB",
                objectKey
        );
    }

    /**
     * MinIO에 영상만 저장하고 objectKey를 반환하는 공통 메서드.
     *
     * 질문별 답변 업로드 API에서 사용한다.
     * 여기서는 FastAPI 분석 요청을 하지 않는다.
     */
    public String uploadVideoToMinio(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("업로드된 파일이 비어 있습니다.");
        }

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "interview.webm";
        }

        String savedFileName = UUID.randomUUID() + "_" + originalFileName;
        String objectKey = "videos/" + savedFileName;

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

            log.info(
                    "MinIO 업로드 성공: bucket={}, objectKey={}, contentType={}",
                    bucketName,
                    objectKey,
                    contentType
            );

            return objectKey;

        } catch (Exception e) {
            log.error("MinIO 업로드 실패: {}", e.getMessage(), e);
            throw new IOException("MinIO 업로드 실패: " + e.getMessage(), e);
        }
    }

    private String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "video/webm";
        }

        // 예: "video/webm;codecs=vp8,opus" -> "video/webm"
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

    private void sendToFastApi(VideoUrlRequest request) {
        String fastApiUrl = fastApiBaseUrl + "/VideoStorage/save";

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        fastApiUrl,
                        request,
                        String.class
                );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("FastAPI 서버 응답 실패: " + response.getStatusCode());
        }

        log.info("FastAPI 분석 요청 성공: {}", response.getBody());
    }
}