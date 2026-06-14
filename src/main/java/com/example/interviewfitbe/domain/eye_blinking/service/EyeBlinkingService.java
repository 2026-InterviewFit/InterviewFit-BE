package com.example.interviewfitbe.domain.eye_blinking.service;

import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReport;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EyeBlinkingService {

    private final EyeBlinkingReportRepository eyeBlinkingReportRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * 저장된 영상의 videoUrl을 받아 FastAPI 눈 깜빡임 분석을 실행하고 DB에 저장한다.
     *
     * 변경 이유:
     * - VideoService가 /videos/upload에서 영상을 MinIO에 저장하고
     *   videoUrl(objectKey, 예: videos/xxx.webm)을 반환한다.
     * - 따라서 eye-blink는 영상을 다시 업로드받거나 Spring에서 MinIO를 다시 다운로드하지 않고,
     *   videoUrl을 그대로 FastAPI에 넘긴다.
     * - FastAPI의 VideoStorage/minio_downloader가 이미 MinIO 다운로드 책임을 가지고 있으므로
     *   Python 쪽에서 objectKey를 실제 임시 파일로 바꾸는 것이 역할 분리상 더 깔끔하다.
     */
    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(EyeBlinkingAnalyzeRequest request) {
        validateVideoUrl(request.getVideoUrl());

        InterviewEntity interview = findInterviewOrNull(request.getInterviewId());

        Map<String, Object> fastApiResult = callFastApi(request);

        EyeBlinkingReport report = EyeBlinkingReport.builder()
                .interview(interview)
                // DB에는 원본 저장 위치를 남긴다.
                // MinIO 기반이면 videos/xxx.webm, 단독 테스트면 로컬 파일 경로가 저장된다.
                .videoUrl(request.getVideoUrl())
                .blinkCount(getInteger(fastApiResult, "blink_count"))
                .blinkRate(getDouble(fastApiResult, "blink_rate"))
                .score(getDouble(fastApiResult, "score"))
                .feedback(getString(fastApiResult, "feedback"))
                .resultJson(toJson(fastApiResult))
                .build();

        EyeBlinkingReport saved = eyeBlinkingReportRepository.save(report);

        return new EyeBlinkingAnalyzeResponse(
                "눈 깜빡임 분석 및 저장 완료",
                saved.getId(),
                fastApiResult
        );
    }

    /**
     * eye-blink 단독 테스트용 multipart 업로드 API에서 사용한다.
     *
     * 최종 통합 흐름에서는 아래 방식이 우선이다.
     * 1) FE → Spring /videos/upload
     * 2) Spring → MinIO 저장 후 videoUrl 반환
     * 3) FE 또는 Spring → /api/eye-blinking/analyze에 videoUrl 전달
     *
     * 그래도 Swagger/개발 중에는 파일 하나를 바로 업로드해서 분석하는 기능이 유용하므로 유지한다.
     */
    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeUploadedVideoAndSave(
            MultipartFile file,
            Long userId,
            Long interviewId
    ) {
        String savedPath = saveVideoFile(file);

        EyeBlinkingAnalyzeRequest request = new EyeBlinkingAnalyzeRequest();
        request.setVideoUrl(savedPath);
        request.setUserId(userId);
        request.setInterviewId(interviewId);

        return analyzeAndSave(request);
    }

    private void validateVideoUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new IllegalArgumentException("videoUrl 값이 필요합니다.");
        }
    }

    /**
     * interviewId가 있으면 실제 InterviewEntity를 조회한다.
     * interviewId가 없으면 eye-blink 단독 테스트로 보고 null 저장을 허용한다.
     */
    private InterviewEntity findInterviewOrNull(Long interviewId) {
        if (interviewId == null) {
            return null;
        }

        return interviewRepository.findById(interviewId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 면접 정보를 찾을 수 없습니다. interviewId=" + interviewId
                ));
    }

    /**
     * FastAPI /api/v1/vision/blinks 호출.
     *
     * request.videoUrl은 두 형태 모두 가능하다.
     * - videos/xxx.webm: FastAPI가 MinIO에서 다운로드한다.
     * - /local/path/xxx.webm: FastAPI가 바로 OpenCV로 읽는다.
     */
    private Map<String, Object> callFastApi(EyeBlinkingAnalyzeRequest request) {
        Map<String, Object> fastApiResult = webClient.post()
                .uri("/api/v1/vision/blinks")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (fastApiResult == null) {
            throw new RuntimeException("FastAPI 눈 깜빡임 분석 결과가 비어 있습니다.");
        }

        return fastApiResult;
    }

    /**
     * 단독 테스트용 로컬 저장 메서드.
     * 팀원 VideoService의 MinIO 저장 흐름을 대체하지 않는다.
     */
    private String saveVideoFile(MultipartFile file) {
        try {
            if (file == null || file.isEmpty()) {
                throw new IOException("업로드된 영상 파일이 비어 있습니다.");
            }

            File dir = new File(uploadDir + "/eye-blinking");

            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFileName = file.getOriginalFilename();
            String extension = getExtension(originalFileName);
            String savedFileName = UUID.randomUUID() + extension;

            File savedFile = new File(dir, savedFileName);
            file.transferTo(savedFile);

            return savedFile.getAbsolutePath();

        } catch (IOException e) {
            throw new RuntimeException("눈 깜빡임 분석용 영상 저장 실패", e);
        }
    }

    private String getExtension(String fileName) {
        if (fileName == null || !fileName.contains(".")) {
            return ".webm";
        }

        return fileName.substring(fileName.lastIndexOf("."));
    }

    private String toJson(Map<String, Object> data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            throw new RuntimeException("눈 깜빡임 분석 결과 JSON 변환 실패", e);
        }
    }

    private Integer getInteger(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return ((Number) value).intValue();
    }

    private Double getDouble(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return ((Number) value).doubleValue();
    }

    private String getString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if (value == null) return null;
        return value.toString();
    }
}