package com.example.interviewfitbe.domain.eye_blinking.service;

import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReport;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EyeBlinkingService {

    private final EyeBlinkingReportRepository eyeBlinkingReportRepository;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(EyeBlinkingAnalyzeRequest request) {

        Map<String, Object> fastApiResult = callFastApi(request);

        EyeBlinkingReport report = EyeBlinkingReport.builder()
                .userId(request.getUserId())
                .interviewId(request.getInterviewId())
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

    private String saveVideoFile(MultipartFile file) {
        try {
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