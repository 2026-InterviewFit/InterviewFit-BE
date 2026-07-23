package com.example.interviewfitbe.domain.eye_blinking.service;

import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingBadLogEntity;
import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReportEntity;
import com.example.interviewfitbe.domain.eye_blinking.enums.EyeBlinkingEventType;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingBadLogRepository;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.repository.InterviewRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EyeBlinkingService {

    private final EyeBlinkingReportRepository eyeBlinkingReportRepository;
    private final EyeBlinkingBadLogRepository eyeBlinkingBadLogRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper;
    private final EyeBlinkingAnalysisService eyeBlinkingAnalysisService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(
            String videoObjectKey,
            InterviewEntity interview
    ) {
        validateVideoUrl(videoObjectKey);

        Map<String, Object> fastApiResponse =
                eyeBlinkingAnalysisService.analyzeBlinking(videoObjectKey);

        Map<String, Object> analysisResult = extractAnalysisResult(fastApiResponse);

        EyeBlinkingReportEntity report = EyeBlinkingReportEntity.builder()
                .interview(interview)
                .blinkCount(getInteger(analysisResult, "blink_count", "blinkCount"))
                .blinkRate(getDouble(analysisResult, "blink_rate", "blinks_per_minute", "blinkRate"))
                .durationSeconds(getDouble(analysisResult, "duration_seconds", "durationSeconds"))
                .notDetectedCount(getInteger(analysisResult, "not_detected_count", "notDetectedCount"))
                .notDetectedRatio(getDouble(analysisResult, "not_detected_ratio", "notDetectedRatio"))
                .score(getDouble(analysisResult, "score"))
                .feedback(getString(analysisResult, "feedback", "evaluation"))
                .resultJson(toJson(fastApiResponse))
                .build();

        report.assignGrade();

        EyeBlinkingReportEntity saved = eyeBlinkingReportRepository.save(report);
        saveBadLogs(analysisResult, saved);

        return new EyeBlinkingAnalyzeResponse(
                "눈 깜빡임 분석 및 저장 완료",
                saved.getId(),
                analysisResult
        );
    }

    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(EyeBlinkingAnalyzeRequest request) {
        validateVideoUrl(request.getVideoUrl());

        InterviewEntity interview = findInterviewOrNull(request.getInterviewId());

        Map<String, Object> fastApiResponse =
                eyeBlinkingAnalysisService.analyzeBlinking(request.getVideoUrl());

        Map<String, Object> analysisResult = extractAnalysisResult(fastApiResponse);

        EyeBlinkingReportEntity report = EyeBlinkingReportEntity.builder()
                .interview(interview)
                .blinkCount(getInteger(analysisResult, "blink_count", "blinkCount"))
                .blinkRate(getDouble(analysisResult, "blink_rate", "blinks_per_minute", "blinkRate"))
                .durationSeconds(getDouble(analysisResult, "duration_seconds", "durationSeconds"))
                .notDetectedCount(getInteger(analysisResult, "not_detected_count", "notDetectedCount"))
                .notDetectedRatio(getDouble(analysisResult, "not_detected_ratio", "notDetectedRatio"))
                .score(getDouble(analysisResult, "score"))
                .feedback(getString(analysisResult, "feedback", "evaluation"))
                .resultJson(toJson(fastApiResponse))
                .build();

        report.assignGrade();

        EyeBlinkingReportEntity saved = eyeBlinkingReportRepository.save(report);
        saveBadLogs(analysisResult, saved);

        return new EyeBlinkingAnalyzeResponse(
                "눈 깜빡임 분석 및 저장 완료",
                saved.getId(),
                analysisResult
        );
    }

    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(
            String videoObjectKey,
            InterviewAnswerEntity interviewAnswer
    ) {
        validateVideoUrl(videoObjectKey);

        Map<String, Object> fastApiResponse =
                eyeBlinkingAnalysisService.analyzeBlinking(videoObjectKey);

        Map<String, Object> analysisResult = extractAnalysisResult(fastApiResponse);

        InterviewEntity interview = interviewAnswer.getInterviewSafely();

        EyeBlinkingReportEntity report = EyeBlinkingReportEntity.builder()
                .interview(interview)
                .interviewAnswer(interviewAnswer)
                .blinkCount(getInteger(analysisResult, "blink_count", "blinkCount"))
                .blinkRate(getDouble(analysisResult, "blink_rate", "blinks_per_minute", "blinkRate"))
                .durationSeconds(getDouble(analysisResult, "duration_seconds", "durationSeconds"))
                .notDetectedCount(getInteger(analysisResult, "not_detected_count", "notDetectedCount"))
                .notDetectedRatio(getDouble(analysisResult, "not_detected_ratio", "notDetectedRatio"))
                .score(getDouble(analysisResult, "score"))
                .feedback(getString(analysisResult, "feedback", "evaluation"))
                .resultJson(toJson(fastApiResponse))
                .build();

        report.assignGrade();

        EyeBlinkingReportEntity saved = eyeBlinkingReportRepository.save(report);
        saveBadLogs(analysisResult, saved);

        return new EyeBlinkingAnalyzeResponse(
                "질문별 눈 깜빡임 분석 및 저장 완료",
                saved.getId(),
                analysisResult
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

    private void validateVideoUrl(String videoUrl) {
        if (videoUrl == null || videoUrl.isBlank()) {
            throw new IllegalArgumentException("videoUrl 값이 필요합니다.");
        }
    }

    private InterviewEntity findInterviewOrNull(Long interviewId) {
        if (interviewId == null) {
            return null;
        }

        return interviewRepository.findById(interviewId)
                .orElseThrow(() -> new EntityNotFoundException(
                        "해당 면접 정보를 찾을 수 없습니다. interviewId=" + interviewId
                ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAnalysisResult(Map<String, Object> fastApiResponse) {
        Object result = fastApiResponse.get("result");

        if (result instanceof Map<?, ?>) {
            return (Map<String, Object>) result;
        }

        return fastApiResponse;
    }

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

    private Integer getInteger(Map<?, ?> map, String... keys) {
        Object value = getFirstValue(map, keys);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Double getDouble(Map<?, ?> map, String... keys) {
        Object value = getFirstValue(map, keys);

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String getString(Map<?, ?> map, String... keys) {
        Object value = getFirstValue(map, keys);

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private void saveBadLogs(Map<String, Object> analysisResult, EyeBlinkingReportEntity report) {
        Object value = getFirstValue(
                analysisResult,
                "bad_logs",
                "badLogs",
                "eye_blinking_bad_logs",
                "eyeBlinkingBadLogs"
        );

        if (!(value instanceof List<?> logs) || logs.isEmpty()) {
            return;
        }

        List<EyeBlinkingBadLogEntity> entities = new ArrayList<>();

        for (Object item : logs) {
            if (!(item instanceof Map<?, ?> logMap)) {
                continue;
            }

            Double timestamp = getDouble(
                    logMap,
                    "timestamp",
                    "time",
                    "start_time",
                    "startTime"
            );

            if (timestamp == null) {
                continue;
            }

            EyeBlinkingEventType eventType = toEyeBlinkEventType(getFirstValue(
                    logMap,
                    "event_type",
                    "eventType",
                    "type"
            ));

            Double duration = getDouble(
                    logMap,
                    "duration",
                    "duration_seconds",
                    "durationSeconds"
            );

            Double blinkRateLocal = getDouble(
                    logMap,
                    "blink_rate_local",
                    "blinkRateLocal",
                    "local_blink_rate",
                    "localBlinkRate"
            );

            EyeBlinkingBadLogEntity badLog = EyeBlinkingBadLogEntity.builder()
                    .eyeBlinkingReportEntity(report)
                    .timestamp(timestamp)
                    .duration(duration)
                    .eventType(eventType)
                    .blinkRateLocal(blinkRateLocal)
                    .build();

            entities.add(badLog);
        }

        if (!entities.isEmpty()) {
            eyeBlinkingBadLogRepository.saveAll(entities);
        }
    }

    private EyeBlinkingEventType toEyeBlinkEventType(Object value) {
        if (value == null) {
            return EyeBlinkingEventType.LONG_CLOSED;
        }

        try {
            return EyeBlinkingEventType.valueOf(value.toString());
        } catch (IllegalArgumentException e) {
            return EyeBlinkingEventType.LONG_CLOSED;
        }
    }

    private Object getFirstValue(Map<?, ?> map, String... keys) {
        for (String key : keys) {
            if (map.containsKey(key)) {
                Object value = map.get(key);

                if (value != null) {
                    return value;
                }
            }
        }

        return null;
    }
}