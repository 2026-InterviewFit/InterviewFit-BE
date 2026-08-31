package com.example.interviewfitbe.domain.video_storage.service;

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
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
public class MinioVideoStreamingService {
    private final MinioClient minioClient;

    @Value("${minio.bucket}")
    private String bucketName;

    public ResponseEntity<StreamingResponseBody> stream(String objectKey, String rangeHeader) {
        if (objectKey == null || objectKey.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "objectKey가 필요합니다.");
        }
        String normalizedObjectKey = normalizeObjectKey(objectKey);
        try {
            StatObjectResponse stat = minioClient.statObject(StatObjectArgs.builder()
                    .bucket(bucketName).object(normalizedObjectKey).build());
            long fileSize = stat.size();
            if (fileSize <= 0) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "영상 파일이 존재하지 않습니다.");
            }
            Range range = parseRange(rangeHeader, fileSize);
            GetObjectArgs getObjectArgs = GetObjectArgs.builder()
                    .bucket(bucketName).object(normalizedObjectKey)
                    .offset(range.start()).length(range.length()).build();
            StreamingResponseBody body = outputStream -> {
                try (InputStream inputStream = minioClient.getObject(getObjectArgs)) {
                    StreamUtils.copy(inputStream, outputStream);
                } catch (Exception exception) {
                    throw new IOException("MinIO 영상 스트리밍 중 오류가 발생했습니다.", exception);
                }
            };

            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.ACCEPT_RANGES, "bytes");
            headers.setContentType(resolveMediaType(normalizedObjectKey, stat.contentType()));
            headers.setContentLength(range.length());
            headers.set(HttpHeaders.CONTENT_DISPOSITION,
                    "inline; filename=\"" + encodeFileName(getFileName(normalizedObjectKey)) + "\"");
            if (range.partial()) {
                headers.set(HttpHeaders.CONTENT_RANGE,
                        "bytes " + range.start() + "-" + range.end() + "/" + fileSize);
            }
            return new ResponseEntity<>(body, headers,
                    range.partial() ? HttpStatus.PARTIAL_CONTENT : HttpStatus.OK);
        } catch (ResponseStatusException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "영상 파일을 찾을 수 없습니다.", exception);
        }
    }

    private String normalizeObjectKey(String objectKey) {
        String normalized = objectKey.trim();
        while (normalized.startsWith("/")) normalized = normalized.substring(1);
        return normalized;
    }

    private Range parseRange(String rangeHeader, long fileSize) {
        if (rangeHeader == null || rangeHeader.isBlank() || !rangeHeader.startsWith("bytes=")) {
            return new Range(0, fileSize - 1, fileSize, false);
        }
        try {
            String[] parts = rangeHeader.substring("bytes=".length()).trim().split("-", 2);
            long start = Long.parseLong(parts[0]);
            long end = parts.length > 1 && !parts[1].isBlank()
                    ? Long.parseLong(parts[1]) : fileSize - 1;
            if (start < 0 || end >= fileSize || start > end) {
                return new Range(0, fileSize - 1, fileSize, false);
            }
            return new Range(start, end, end - start + 1, true);
        } catch (Exception ignored) {
            return new Range(0, fileSize - 1, fileSize, false);
        }
    }

    private MediaType resolveMediaType(String objectKey, String contentType) {
        if (objectKey.endsWith(".webm")) return MediaType.parseMediaType("video/webm");
        if (objectKey.endsWith(".mp4")) return MediaType.parseMediaType("video/mp4");
        if (contentType != null && !contentType.isBlank()) {
            try {
                return MediaType.parseMediaType(contentType);
            } catch (Exception ignored) {
                return MediaType.APPLICATION_OCTET_STREAM;
            }
        }
        return MediaType.APPLICATION_OCTET_STREAM;
    }

    private String getFileName(String objectKey) {
        int slashIndex = objectKey.lastIndexOf('/');
        return slashIndex >= 0 && slashIndex < objectKey.length() - 1
                ? objectKey.substring(slashIndex + 1) : objectKey;
    }

    private String encodeFileName(String fileName) {
        return URLEncoder.encode(fileName, StandardCharsets.UTF_8).replace("+", "%20");
    }

    private record Range(long start, long end, long length, boolean partial) {}
}
