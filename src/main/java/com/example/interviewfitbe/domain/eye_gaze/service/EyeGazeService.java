package com.example.interviewfitbe.domain.eye_gaze.service;

import com.example.interviewfitbe.domain.eye_gaze.dto.EyeGazeAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_gaze.dto.EyeGazeAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_gaze.entity.EyeGazeBadLogEntity;
import com.example.interviewfitbe.domain.eye_gaze.entity.EyeGazeReport;
import com.example.interviewfitbe.domain.eye_gaze.enums.EyeGazeGrade;
import com.example.interviewfitbe.domain.eye_gaze.enums.GazeDirection;
import com.example.interviewfitbe.domain.eye_gaze.repository.EyeGazeBadLogRepository;
import com.example.interviewfitbe.domain.eye_gaze.repository.EyeGazeReportRepository;
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
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class EyeGazeService {

    private final EyeGazeReportRepository eyeGazeReportRepository;
    private final EyeGazeBadLogRepository eyeGazeBadLogRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper;
    private final EyeGazeAnalysisService eyeGazeAnalysisService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    /**
     * MinIO objectKey 기반 시선 분석
     */
    @Transactional
    public EyeGazeAnalyzeResponse analyzeAndSave(
            String videoObjectKey,
            InterviewEntity interview
    ) {

        validateVideoUrl(videoObjectKey);

        if (interview == null) {
            throw new IllegalArgumentException(
                    "interview 정보가 필요합니다."
            );
        }

        Map<String, Object> fastApiResponse =
                eyeGazeAnalysisService.analyzeGaze(
                        videoObjectKey
                );

        Map<String, Object> analysisResult =
                extractAnalysisResult(
                        fastApiResponse
                );

        EyeGazeReport saved =
                saveReport(
                        interview,
                        analysisResult,
                        fastApiResponse
                );

        saveBadLogs(
                saved,
                analysisResult
        );

        return new EyeGazeAnalyzeResponse(
                "시선 분석 및 저장 완료",
                saved.getId(),
                analysisResult
        );
    }

    /**
     * 요청 DTO 기반 시선 분석
     */
    @Transactional
    public EyeGazeAnalyzeResponse analyzeAndSave(
            EyeGazeAnalyzeRequest request
    ) {

        validateVideoUrl(
                request.getVideoUrl()
        );

        InterviewEntity interview =
                findInterview(
                        request.getInterviewId()
                );

        Map<String, Object> fastApiResponse =
                eyeGazeAnalysisService.analyzeGaze(
                        request.getVideoUrl()
                );

        Map<String, Object> analysisResult =
                extractAnalysisResult(
                        fastApiResponse
                );

        EyeGazeReport saved =
                saveReport(
                        interview,
                        analysisResult,
                        fastApiResponse
                );

        saveBadLogs(
                saved,
                analysisResult
        );

        return new EyeGazeAnalyzeResponse(
                "시선 분석 및 저장 완료",
                saved.getId(),
                analysisResult
        );
    }

    /**
     * 직접 업로드한 영상 분석
     */
    @Transactional
    public EyeGazeAnalyzeResponse analyzeUploadedVideoAndSave(
            MultipartFile file,
            Long userId,
            Long interviewId
    ) {

        String savedPath =
                saveVideoFile(file);

        EyeGazeAnalyzeRequest request =
                new EyeGazeAnalyzeRequest();

        request.setVideoUrl(savedPath);
        request.setUserId(userId);
        request.setInterviewId(interviewId);

        return analyzeAndSave(request);
    }

    /**
     * 시선 분석 리포트 저장
     */
    private EyeGazeReport saveReport(
            InterviewEntity interview,
            Map<String, Object> analysisResult,
            Map<String, Object> fastApiResponse
    ) {

        Double score =
                getDouble(
                        analysisResult,
                        "score"
                );

        EyeGazeReport report =
                EyeGazeReport.builder()

                        .interview(interview)

                        .durationSeconds(
                                getDouble(
                                        analysisResult,
                                        "duration_seconds"
                                )
                        )

                        .centerRatio(
                                normalizeRatio(
                                        getDouble(
                                                analysisResult,
                                                "center_ratio"
                                        )
                                )
                        )

                        .gazeAwayRatio(
                                normalizeRatio(
                                        getDouble(
                                                analysisResult,
                                                "gaze_away_ratio"
                                        )
                                )
                        )

                        .gazeAwayCount(
                                getInteger(
                                        analysisResult,
                                        "gaze_away_count"
                                )
                        )

                        .notDetectedCount(
                                getInteger(
                                        analysisResult,
                                        "not_detected_count"
                                )
                        )

                        .notDetectedRatio(
                                normalizeRatio(
                                        getDouble(
                                                analysisResult,
                                                "not_detected_ratio"
                                        )
                                )
                        )

                        .score(score)

                        .grade(
                                calculateGrade(score)
                        )

                        .evaluation(
                                getString(
                                        analysisResult,
                                        "evaluation"
                                )
                        )

                        .resultJson(
                                toJson(
                                        fastApiResponse
                                )
                        )

                        .build();

        return eyeGazeReportRepository.save(
                report
        );
    }

    /**
     * FastAPI의 bad_logs를 DB에 저장
     */
    @SuppressWarnings("unchecked")
    private void saveBadLogs(
            EyeGazeReport report,
            Map<String, Object> analysisResult
    ) {

        Object badLogsObject =
                analysisResult.get(
                        "bad_logs"
                );

        if (!(badLogsObject instanceof List<?>)) {
            return;
        }

        List<?> badLogs =
                (List<?>) badLogsObject;

        for (Object item : badLogs) {

            if (!(item instanceof Map<?, ?>)) {
                continue;
            }

            Map<String, Object> log =
                    (Map<String, Object>) item;

            String directionValue =
                    getString(
                            log,
                            "direction"
                    );

            GazeDirection direction =
                    parseDirection(
                            directionValue
                    );

            EyeGazeBadLogEntity badLog =
                    EyeGazeBadLogEntity.builder()

                            .eyeGazeReport(
                                    report
                            )

                            .timestamp(
                                    getDouble(
                                            log,
                                            "timestamp"
                                    )
                            )

                            .duration(
                                    getDouble(
                                            log,
                                            "duration"
                                    )
                            )

                            .direction(
                                    direction
                            )

                            .deviation(
                                    getDouble(
                                            log,
                                            "deviation"
                                    )
                            )

                            .build();

            eyeGazeBadLogRepository.save(
                    badLog
            );
        }
    }

    private GazeDirection parseDirection(
            String value
    ) {

        if (
                value == null
                        || value.isBlank()
        ) {
            return null;
        }

        try {
            return GazeDirection.valueOf(
                    value.trim().toUpperCase()
            );

        } catch (IllegalArgumentException e) {

            return null;
        }
    }

    private void validateVideoUrl(
            String videoUrl
    ) {

        if (
                videoUrl == null
                        || videoUrl.isBlank()
        ) {

            throw new IllegalArgumentException(
                    "videoUrl 값이 필요합니다."
            );
        }
    }

    private InterviewEntity findInterview(
            Long interviewId
    ) {

        if (interviewId == null) {

            throw new IllegalArgumentException(
                    "interviewId 값이 필요합니다."
            );
        }

        return interviewRepository
                .findById(interviewId)
                .orElseThrow(
                        () ->
                                new EntityNotFoundException(
                                        "해당 면접 정보를 찾을 수 없습니다. interviewId="
                                                + interviewId
                                )
                );
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractAnalysisResult(
            Map<String, Object> fastApiResponse
    ) {

        Object result =
                fastApiResponse.get(
                        "result"
                );

        if (result instanceof Map<?, ?>) {

            return (Map<String, Object>) result;
        }

        return fastApiResponse;
    }

    /**
     * FastAPI가 0~100 비율을 반환하면
     * DB 저장 시 0~1로 변환한다.
     */
    private Double normalizeRatio(
            Double value
    ) {

        if (value == null) {
            return null;
        }

        if (value > 1.0) {
            return value / 100.0;
        }

        return value;
    }

    private EyeGazeGrade calculateGrade(
            Double score
    ) {

        if (score == null) {
            return null;
        }

        if (score >= 90) {
            return EyeGazeGrade.EXCELLENT;
        }

        if (score >= 80) {
            return EyeGazeGrade.GOOD;
        }

        if (score >= 70) {
            return EyeGazeGrade.AVERAGE;
        }

        if (score >= 60) {
            return EyeGazeGrade.NEEDS_IMPROVEMENT;
        }

        return EyeGazeGrade.POOR;
    }

    private String saveVideoFile(
            MultipartFile file
    ) {

        try {

            if (
                    file == null
                            || file.isEmpty()
            ) {

                throw new IOException(
                        "업로드된 영상 파일이 비어 있습니다."
                );
            }

            File dir =
                    new File(
                            uploadDir
                                    + "/eye-gaze"
                    );

            if (!dir.exists()) {

                boolean created =
                        dir.mkdirs();

                if (!created && !dir.exists()) {

                    throw new IOException(
                            "업로드 디렉터리를 생성할 수 없습니다."
                    );
                }
            }

            String originalFileName =
                    file.getOriginalFilename();

            String extension =
                    getExtension(
                            originalFileName
                    );

            String savedFileName =
                    UUID.randomUUID()
                            + extension;

            File savedFile =
                    new File(
                            dir,
                            savedFileName
                    );

            file.transferTo(
                    savedFile
            );

            return savedFile
                    .getAbsolutePath();

        } catch (IOException e) {

            throw new RuntimeException(
                    "시선 분석용 영상 저장 실패",
                    e
            );
        }
    }

    private String getExtension(
            String fileName
    ) {

        if (
                fileName == null
                        || !fileName.contains(".")
        ) {

            return ".webm";
        }

        return fileName.substring(
                fileName.lastIndexOf(".")
        );
    }

    private String toJson(
            Map<String, Object> data
    ) {

        try {

            return objectMapper
                    .writeValueAsString(
                            data
                    );

        } catch (Exception e) {

            throw new RuntimeException(
                    "시선 분석 결과 JSON 변환 실패",
                    e
            );
        }
    }

    private Integer getInteger(
            Map<String, Object> map,
            String... keys
    ) {

        Object value =
                getFirstValue(
                        map,
                        keys
                );

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.intValue();
        }

        return Integer.valueOf(
                value.toString()
        );
    }

    private Double getDouble(
            Map<String, Object> map,
            String... keys
    ) {

        Object value =
                getFirstValue(
                        map,
                        keys
                );

        if (value == null) {
            return null;
        }

        if (value instanceof Number number) {
            return number.doubleValue();
        }

        return Double.valueOf(
                value.toString()
        );
    }

    private String getString(
            Map<String, Object> map,
            String... keys
    ) {

        Object value =
                getFirstValue(
                        map,
                        keys
                );

        if (value == null) {
            return null;
        }

        return value.toString();
    }

    private Object getFirstValue(
            Map<String, Object> map,
            String... keys
    ) {

        for (String key : keys) {

            Object value =
                    map.get(key);

            if (value != null) {
                return value;
            }
        }

        return null;
    }
}