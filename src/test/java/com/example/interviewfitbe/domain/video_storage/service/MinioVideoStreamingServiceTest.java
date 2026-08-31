package com.example.interviewfitbe.domain.video_storage.service;

import io.minio.MinioClient;
import io.minio.StatObjectResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class MinioVideoStreamingServiceTest {
    private MinioClient minioClient;
    private MinioVideoStreamingService service;

    @BeforeEach
    void setUp() {
        minioClient = mock(MinioClient.class);
        service = new MinioVideoStreamingService(minioClient);
        ReflectionTestUtils.setField(service, "bucketName", "videos");
    }

    @Test
    void rangeRequestReturnsPartialContentAndSeekHeaders() throws Exception {
        StatObjectResponse stat = mock(StatObjectResponse.class);
        when(stat.size()).thenReturn(1_000L);
        when(stat.contentType()).thenReturn("video/webm");
        when(minioClient.statObject(any())).thenReturn(stat);

        var response = service.stream("videos/answer.webm", "bytes=100-299");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.PARTIAL_CONTENT);
        assertThat(response.getHeaders().getFirst(HttpHeaders.ACCEPT_RANGES)).isEqualTo("bytes");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_RANGE))
                .isEqualTo("bytes 100-299/1000");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(200L);
        assertThat(response.getHeaders().getContentType().toString()).isEqualTo("video/webm");
    }

    @Test
    void missingMinioObjectReturnsNotFound() throws Exception {
        when(minioClient.statObject(any())).thenThrow(new RuntimeException("NoSuchKey"));

        assertThatThrownBy(() -> service.stream("videos/missing.webm", null))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
