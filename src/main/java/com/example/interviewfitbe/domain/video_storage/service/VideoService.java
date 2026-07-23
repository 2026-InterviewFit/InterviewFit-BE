package com.example.interviewfitbe.domain.video_storage.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.example.interviewfitbe.domain.video_storage.dto.VideoUrlResponse;
import com.example.interviewfitbe.domain.video_storage.entity.VideoEntity;
import com.example.interviewfitbe.domain.video_storage.entity.VideoType;
import com.example.interviewfitbe.domain.video_storage.repository.VideoRepository;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
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

    private final MinioClient minioClient;
    private final VideoRepository videoRepository;
    private final InterviewRepository interviewRepository;

    @Value("${minio.bucket}")
    private String bucketName;

    /**
     * 영상 MinIO 업로드 + videos 테이블 저장
     */
    public VideoUrlResponse saveVideoFile(
            Long interviewId,
            MultipartFile file,
            VideoType videoType
    ) throws IOException {

        validateFile(file);

        InterviewEntity interview =
                interviewRepository.findById(interviewId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "존재하지 않는 인터뷰입니다. interviewId="
                                                + interviewId
                                )
                        );

        String originalFileName =
                getOriginalFileName(file);

        String contentType =
                normalizeContentType(
                        file.getContentType()
                );

        String objectKey =
                uploadVideoToMinio(
                        file,
                        originalFileName,
                        contentType
                );

        saveVideoMetadata(
                interview,
                file,
                originalFileName,
                objectKey,
                contentType,
                videoType
        );

        return new VideoUrlResponse(
                "Video file saved to MinIO and DB",
                objectKey
        );
    }

    /**
     * MinIO에 영상만 업로드할 때 사용
     */
    public String uploadVideoToMinio(
            MultipartFile file
    ) throws IOException {

        validateFile(file);

        String originalFileName =
                getOriginalFileName(file);

        String contentType =
                normalizeContentType(
                        file.getContentType()
                );

        return uploadVideoToMinio(
                file,
                originalFileName,
                contentType
        );
    }

    private String uploadVideoToMinio(
            MultipartFile file,
            String originalFileName,
            String contentType
    ) throws IOException {

        String objectKey =
                createObjectKey(
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
                    "MinIO 업로드 성공: bucket={}, objectKey={}, contentType={}",
                    bucketName,
                    objectKey,
                    contentType
            );

            return objectKey;

        } catch (Exception e) {

            log.error(
                    "MinIO 업로드 실패: {}",
                    e.getMessage(),
                    e
            );

            throw new IOException(
                    "MinIO 업로드 실패: "
                            + e.getMessage(),
                    e
            );
        }
    }

    /**
     * videos 테이블 저장
     */
    private VideoEntity saveVideoMetadata(
            InterviewEntity interview,
            MultipartFile file,
            String originalFileName,
            String objectKey,
            String contentType,
            VideoType videoType
    ) {

        VideoEntity video =
                VideoEntity.builder()

                        .interview(interview)

                        .originalFileName(
                                originalFileName
                        )

                        .objectKey(
                                objectKey
                        )

                        .videoType(
                                videoType
                        )

                        /*
                         * MultipartFile만으로는 정확한 영상 재생 시간을
                         * 바로 알 수 없으므로 우선 0.0 저장.
                         *
                         * 추후 FFmpeg 또는 FastAPI 분석 결과로
                         * 실제 durationSeconds를 업데이트할 수 있음.
                         */
                        .durationSeconds(
                                0.0
                        )

                        .contentType(
                                contentType
                        )

                        .fileSize(
                                file.getSize()
                        )

                        .build();

        return videoRepository.save(
                video
        );
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
    }

    private String getOriginalFileName(
            MultipartFile file
    ) {

        String originalFileName =
                file.getOriginalFilename();

        if (
                originalFileName == null
                        || originalFileName.isBlank()
        ) {

            return DEFAULT_VIDEO_FILE_NAME;
        }

        return originalFileName;
    }

    private String createObjectKey(
            String originalFileName
    ) {

        return VIDEO_DIRECTORY
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

            return DEFAULT_CONTENT_TYPE;
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
}