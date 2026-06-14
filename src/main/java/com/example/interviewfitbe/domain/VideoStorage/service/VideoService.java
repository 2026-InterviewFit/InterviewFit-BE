package com.example.interviewfitbe.domain.VideoStorage.service;

import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlRequest;
import com.example.interviewfitbe.domain.VideoStorage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.VideoStorage.entity.VideoEntity;
import com.example.interviewfitbe.domain.VideoStorage.repository.VideoRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

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

        VideoEntity video = new VideoEntity();
        video.setOriginalFileName(originalFileName);
        video.setObjectKey(objectKey);
        videoRepository.save(video);

        VideoUrlRequest request = new VideoUrlRequest();
        request.setVideoUrl(objectKey);

        try {
            sendToFastApi(request);
        } catch (Exception e) {
            System.out.println("FastAPI 전송 실패: " + e.getMessage());
        }

        return new VideoUrlResponse(
                "Video file saved to MinIO and DB",
                objectKey
        );
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

        String fastApiUrl = fastApiBaseUrl + "/VideoStorage/save";

        ResponseEntity<String> response =
                restTemplate.postForEntity(
                        fastApiUrl,
                        request,
                        String.class
                );

        System.out.println("FastAPI 응답: " + response.getBody());


    }

}