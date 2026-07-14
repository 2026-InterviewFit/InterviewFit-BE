package com.example.interviewfitbe.domain.video_storage.controller;

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
import org.springframework.util.StreamUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/videos")
public class VideoStreamController {

    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    /**
     * MinIO에 저장된 영상을 브라우저에서 다시 볼 수 있게 스트리밍한다.
     *
     * 예:
     * GET /api/videos/stream?objectKey=videos/abc_answer-1.webm
     */
    @GetMapping("/stream")
    public ResponseEntity<StreamingResponseBody> streamVideo(
            @RequestParam String objectKey,
            @RequestHeader(value = "Range", required = false) String rangeHeader
    ) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectKey가 필요합니다.");
        }

        String normalizedObjectKey = normalizeObjectKey(objectKey);

        try {
            StatObjectResponse stat = minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(normalizedObjectKey)
                            .build()
            );

            long fileSize = stat.size();

            if (fileSize <= 0) {
                throw new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "영상 파일 크기가 0입니다. objectKey=" + normalizedObjectKey
                );
            }

            Range range = parseRange(rangeHeader, fileSize);

            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucketName)
                    .object(normalizedObjectKey)
                    .offset(range.start())
                    .length(range.length())
                    .build();

            StreamingResponseBody responseBody = outputStream -> {
                try (InputStream inputStream = minioClient.getObject(getObjectArgs)) {
                    StreamUtils.copy(inputStream, outputStream);
                } catch (Exception e) {
                    throw new IOException("MinIO 영상 스트리밍 중 오류가 발생했습니다.", e);
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.setContentType(resolveMediaType(normalizedObjectKey, stat.contentType()));
            headers.setContentLength(range.length());
            headers.set(
                    HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + encodeFileName(getFileName(normalizedObjectKey)) + "\""
            );

            if (range.partial()) {
                headers.set(
                        HttpHeaders.CONTENT_RANGE,
                        "bytes " + range.start() + "-" + range.end() + "/" + fileSize
                );

                return new ResponseEntity<>(responseBody, headers, HttpStatus.PARTIAL_CONTENT);
            }

            return new ResponseEntity<>(responseBody, headers, HttpStatus.OK);

        } catch (ResponseStatusException e) {
            throw e;

        } catch (Exception e) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "영상을 찾을 수 없습니다. objectKey=" + normalizedObjectKey,
                    e
            );
        }
    }

    private String normalizeObjectKey(String objectKey) {
        String normalized = objectKey.trim();

        while (normalized.startsWith("/")) {
            normalized = normalized.substring(1);
        }

        return normalized;
    }

    private MediaType resolveMediaType(String objectKey, String contentType) {
        if (objectKey.endsWith(".webm")) {
            return MediaType.parseMediaType("video/webm");
        }

        if (objectKey.endsWith(".mp4")) {
            return MediaType.parseMediaType("video/mp4");
        }

        if (contentType != null && !contentType.isBlank()) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (Exception ignored) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
        }

        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private Range parseRange(String rangeHeader, long fileSize) {
        if (rangeHeader == null || rangeHeader.isBlank() || !rangeHeader.startsWith("bytes=")) {
            return new Range(0, fileSize - 1, fileSize, false);
        }

        try {
            String rangeValue = rangeHeader.replace("bytes=", "").trim();
            String[] parts = rangeValue.split("-", 2);

            long start = Long.parseLong(parts[0]);
            long end = fileSize - 1;

            if (parts.length > 1 && !parts[1].isBlank()) {
                end = Long.parseLong(parts[1]);
            }

            if (start < 0 || end >= fileSize || start > end) {
                return new Range(0, fileSize - 1, fileSize, false);
            }

            return new Range(start, end, end - start + 1, true);

        } catch (Exception e) {
            return new Range(0, fileSize - 1, fileSize, false);
        }
    }

    private String getFileName(String objectKey) {
        int slashIndex = objectKey.lastIndexOf("/");

        if (slashIndex >= 0 && slashIndex < objectKey.length() - 1) {
            return objectKey.substring(slashIndex + 1);
        }

        return objectKey;
    }

    private String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8)
                .replace("+", "%20");
    }

    private record Range(
            long start,
            long end,
            long length,
            boolean partial
    ) {
    }
}