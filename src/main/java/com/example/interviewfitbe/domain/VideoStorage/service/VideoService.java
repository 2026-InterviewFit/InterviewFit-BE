package com.example.interviewfitbe.domain.VideoStorage.service;

import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.VideoStorage.entity.VideoEntity;
import com.example.interviewfitbe.domain.VideoStorage.repository.VideoRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.extern.slf4j.Slf4j; // 로그 기록을 위해 추가
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

@Slf4j // 로그 사용을 위한 어노테이션
@Service
public class VideoService {

    private final MinioClient minioClient;
    private final VideoRepository videoRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    public VideoService(MinioClient minioClient, VideoRepository videoRepository) {
        this.minioClient = minioClient;
        this.videoRepository = videoRepository;
    }

    public VideoUrlResponse saveVideoFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IOException("업로드된 파일이 비어 있습니다.");
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "video.mp4";
        }

        String savedFileName = UUID.randomUUID() + "_" + originalFileName;
        String objectKey = "videos/" + savedFileName;

        try {
            createBucketIfNotExists();

            // 💡 1. Content-Type 정제: 세미콜론 이후의 codecs 정보를 제거하여 RFC 규격 준수
            String contentType = file.getContentType();
            if (contentType != null && contentType.contains(";")) {
                contentType = contentType.split(";")[0];
            }

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(contentType)
                            .build()
            );

        } catch (Exception e) {
            log.error("MinIO 업로드 실패: {}", e.getMessage());
            throw new IOException("MinIO 업로드 실패", e);
        }

        // DB 저장
        VideoEntity video = new VideoEntity();
        video.setOriginalFileName(originalFileName);
        video.setObjectKey(objectKey);
        videoRepository.save(video);

        // FastAPI 연동
        VideoUrlRequest request = new VideoUrlRequest();
        request.setVideoUrl(objectKey);

        try {
            sendToFastApi(request);
        } catch (Exception e) {
            log.error("FastAPI 분석 요청 실패: {}", e.getMessage());
            // 필요하다면 여기서 분석 실패 상태를 DB에 기록
        }

        return new VideoUrlResponse("Video file saved to MinIO and DB", objectKey);
    }

    private void createBucketIfNotExists() throws Exception {
        boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
        if (!exists) {
            minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
        }
    }

    private void sendToFastApi(VideoUrlRequest request) {
        String fastApiUrl = fastApiBaseUrl + "/VideoStorage/save";
        ResponseEntity<String> response = restTemplate.postForEntity(fastApiUrl, request, String.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("FastAPI 서버 응답 실패: " + response.getStatusCode());
        }
        log.info("FastAPI 분석 요청 성공: {}", response.getBody());
    }
}