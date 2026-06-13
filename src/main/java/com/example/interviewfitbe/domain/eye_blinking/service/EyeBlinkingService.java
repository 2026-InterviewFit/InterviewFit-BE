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

    /*
     * 이미 저장된 videoUrl을 받아서 FastAPI 분석 후 DB 저장.
     * request.interviewId가 있으면 InterviewEntity와 연결한다.
     */
    @Transactional
    public EyeBlinkingAnalyzeResponse analyzeAndSave(EyeBlinkingAnalyzeRequest request) {

        InterviewEntity interview = findInterviewOrNull(request.getInterviewId());

        Map<String, Object> fastApiResult = callFastApi(request);

        EyeBlinkingReport report = EyeBlinkingReport.builder()
                .interview(interview)
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

    /*
     * FE에서 multipart 영상 파일을 업로드하면
     * Spring 서버에 저장한 뒤 FastAPI에 저장 경로를 넘긴다.
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

        /*
         * userId는 현재 request 호환용으로만 받는다.
         * 실제 저장 기준은 interviewId → InterviewEntity 관계다.
         */
        request.setUserId(userId);
        request.setInterviewId(interviewId);

        return analyzeAndSave(request);
    }

    /*
     * interviewId가 있으면 실제 InterviewEntity를 조회한다.
     * 없으면 단독 테스트로 보고 null 허용.
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

    /*
     * FastAPI /api/v1/vision/blinks 호출.
     * WebClientConfig의 fastapi.base-url을 baseUrl로 사용한다.
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

    /*
     * 업로드된 영상을 로컬 파일로 저장한다.
     *
     * Docker로 백엔드와 FastAPI를 각각 컨테이너로 띄울 경우,
     * 이 경로는 두 컨테이너가 모두 볼 수 있는 shared volume이어야 한다.
     */
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