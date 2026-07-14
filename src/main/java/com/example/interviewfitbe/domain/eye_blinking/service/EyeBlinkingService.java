package com.example.interviewfitbe.domain.eye_blinking.service;

import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_blinking.dto.EyeBlinkingAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_blinking.entity.EyeBlinkingReport;
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
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EyeBlinkingService {

    private final EyeBlinkingReportRepository eyeBlinkingReportRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper;

    /**
     * FastAPI 눈 깜빡임 분석 호출 전담 서비스.
     */
    private final EyeBlinkingAnalysisService eyeBlinkingAnalysisService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * 면접 단위 눈 깜빡임 분석 저장 메서드.
     *
     * 예전 통합 흐름에서 InterviewEntity만 연결할 때 사용한다.
     */
    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(
            String videoObjectKey,
            InterviewEntity interview
    ) {
        validateVideoUrl(videoObjectKey);

        Map<String, Object> fastApiResponse =
                eyeBlinkingAnalysisService.analyzeBlinking(videoObjectKey);

        Map<String, Object> analysisResult = extractAnalysisResult(fastApiResponse);

        EyeBlinkingReport report = EyeBlinkingReport.builder()
                .interview(interview)
                .videoUrl(videoObjectKey)
                .blinkCount(getInteger(analysisResult, "blink_count", "blinkCount"))
                .blinkRate(getDouble(analysisResult, "blink_rate", "blinks_per_minute", "blinkRate"))
                .durationSeconds(getDouble(analysisResult, "duration_seconds", "durationSeconds"))
                .score(getDouble(analysisResult, "score"))
                .feedback(getString(analysisResult, "feedback", "evaluation"))
                .resultJson(toJson(fastApiResponse))
                .build();

        EyeBlinkingReport saved = eyeBlinkingReportRepository.save(report);

        return new EyeBlinkingAnalyzeResponse(
                "눈 깜빡임 분석 및 저장 완료",
                saved.getId(),
                analysisResult
        );
    }

    /**
     * eye-blink 단독 분석 API용 메서드.
     *
     * /api/eye-blinking/analyze 에서 videoUrl만 받아 분석할 때 사용한다.
     * interviewId가 있으면 interview_id를 연결하고,
     * 없으면 단독 테스트로 보고 null 저장을 허용한다.
     */
    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(EyeBlinkingAnalyzeRequest request) {
        validateVideoUrl(request.getVideoUrl());

        InterviewEntity interview = findInterviewOrNull(request.getInterviewId());

        Map<String, Object> fastApiResponse =
                eyeBlinkingAnalysisService.analyzeBlinking(request.getVideoUrl());

        Map<String, Object> analysisResult = extractAnalysisResult(fastApiResponse);

        EyeBlinkingReport report = EyeBlinkingReport.builder()
                .interview(interview)
                .videoUrl(request.getVideoUrl())
                .blinkCount(getInteger(analysisResult, "blink_count", "blinkCount"))
                .blinkRate(getDouble(analysisResult, "blink_rate", "blinks_per_minute", "blinkRate"))
                .durationSeconds(getDouble(analysisResult, "duration_seconds", "durationSeconds"))
                .score(getDouble(analysisResult, "score"))
                .feedback(getString(analysisResult, "feedback", "evaluation"))
                .resultJson(toJson(fastApiResponse))
                .build();

        EyeBlinkingReport saved = eyeBlinkingReportRepository.save(report);

        return new EyeBlinkingAnalyzeResponse(
                "눈 깜빡임 분석 및 저장 완료",
                saved.getId(),
                analysisResult
        );
    }

    /**
     * 질문별 답변 영상 눈 깜빡임 분석 저장 메서드.
     *
     * 대시보드에서 answer.eye로 보여주려면 이 메서드처럼
     * InterviewAnswerEntity까지 연결되어야 한다.
     */
    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(
            String videoObjectKey,
            InterviewAnswerEntity interviewAnswer
    ) {
        validateVideoUrl(videoObjectKey);

        Map<String, Object> fastApiResponse =
                eyeBlinkingAnalysisService.analyzeBlinking(videoObjectKey);

        Map<String, Object> analysisResult = extractAnalysisResult(fastApiResponse);

        EyeBlinkingReport report = EyeBlinkingReport.builder()
                .interview(interviewAnswer.getInterview())
                .interviewAnswer(interviewAnswer)
                .videoUrl(videoObjectKey)
                .blinkCount(getInteger(analysisResult, "blink_count", "blinkCount"))
                .blinkRate(getDouble(analysisResult, "blink_rate", "blinks_per_minute", "blinkRate"))
                .durationSeconds(getDouble(analysisResult, "duration_seconds", "durationSeconds"))
                .score(getDouble(analysisResult, "score"))
                .feedback(getString(analysisResult, "feedback", "evaluation"))
                .resultJson(toJson(fastApiResponse))
                .build();

        EyeBlinkingReport saved = eyeBlinkingReportRepository.save(report);

        return new EyeBlinkingAnalyzeResponse(
                "질문별 눈 깜빡임 분석 및 저장 완료",
                saved.getId(),
                analysisResult
        );
    }

    /**
     * eye-blink 단독 테스트용 multipart 업로드 API에서 사용한다.
     *
     * 최종 통합 흐름에서는 이 방식보다
     * /videos/upload → videoObjectKey → 통합 분석 흐름을 우선 사용한다.
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
     * interviewId가 없으면 단독 테스트로 보고 null 저장을 허용한다.
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
     * FastAPI 응답이 두 형태 모두 올 수 있어서 대응한다.
     *
     * 1. 직접 결과 형태:
     * {
     *   "blink_count": 3,
     *   "blink_rate": 20.0,
     *   "score": 80,
     *   "feedback": "..."
     * }
     *
     * 2. wrapper 형태:
     * {
     *   "status": "success",
     *   "result": {
     *     "blink_count": 3,
     *     "blink_rate": 20.0,
     *     "score": 80,
     *     "feedback": "..."
     *   }
     * }
     */
    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAnalysisResult(Map<String, Object> fastApiResponse) {
        Object result = fastApiResponse.get("result");

        if (result instanceof Map<?, ?>) {
            return (Map<String, Object>) result;
        }

        return fastApiResponse;
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

    private Integer getInteger(Map<String, Object> map, String... keys) {
        Object value = getFirstValue(map, keys);

        if (value == null) {
            return null;
        }

        return ((Number) value).intValue();
    }

    private Double getDouble(Map<String, Object> map, String... keys) {
        Object value = getFirstValue(map, keys);

        if (value == null) {
            return null;
        }

        return ((Number) value).doubleValue();
    }

    private String getString(Map<String, Object> map, String... keys) {
        Object value = getFirstValue(map, keys);

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private Object getFirstValue(Map<String, Object> map, String... keys) {
        for (String key : keys) {
            Object value = map.get(key);

            if (value != null) {
                return value;
            }
        }

        return null;
    }
}