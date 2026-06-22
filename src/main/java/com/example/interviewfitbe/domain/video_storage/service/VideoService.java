package com.example.interviewfitbe.domain.video_storage.service;

import com.example.interviewfitbe.domain.video_storage.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.video_storage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.video_storage.entity.VideoEntity;
import com.example.interviewfitbe.domain.video_storage.repository.VideoRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
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

    private final MinioClient minioClient;
    private final VideoRepository videoRepository;
    private final InterviewRepository interviewRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${minio.bucket}")
    private String bucketName;

    @Value("${fastapi.base-url}")
    private String fastApiBaseUrl;

    public VideoUrlResponse saveVideoFile(MultipartFile file, Long interviewId) throws IOException {

        if (file == null || file.isEmpty()) {
            throw new IOException("업로드된 파일이 비어 있습니다.");
        }

        InterviewEntity interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new RuntimeException("면접 정보가 없습니다. interviewId=" + interviewId));

        String originalFileName = file.getOriginalFilename();

        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "video.webm";
        }

        String savedFileName = UUID.randomUUID() + "_" + originalFileName;
        String objectKey = "videos/" + savedFileName;

        try {
            createBucketIfNotExists();

            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType())
                            .build()
            );

        } catch (Exception e) {
            throw new IOException("MinIO 업로드 실패: " + e.getMessage(), e);
        }

        VideoEntity video = VideoEntity.builder()
                .originalFileName(originalFileName)
                .objectKey(objectKey)
                .interview(interview)
                .user(interview.getUser())
                .build();

        videoRepository.save(video);

        VideoUrlRequest request = VideoUrlRequest.builder()
                .videoUrl(objectKey)
                .interviewId(interviewId)
                .build();

        try {
            sendToFastApi(request);
        } catch (Exception e) {
            log.warn("FastAPI 전송 실패: {}", e.getMessage());
        }

        return VideoUrlResponse.builder()
                .message("Video file saved to MinIO and DB")
                .videoUrl(objectKey)
                .build();
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
        }
    }

    private void sendToFastApi(VideoUrlRequest request) {

        String fastApiUrl = fastApiBaseUrl + "/apis/videos/save";

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        fastApiUrl,
                        request,
                        String.class
                );

        log.info("FastAPI 응답: {}", response.getBody());
    }
}