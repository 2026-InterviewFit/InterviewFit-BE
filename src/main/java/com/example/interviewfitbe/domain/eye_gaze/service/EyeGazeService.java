package com.example.interviewfitbe.domain.eye_gaze.service;

import com.example.interviewfitbe.domain.eye_gaze.dto.EyeGazeAnalyzeRequest;
import com.example.interviewfitbe.domain.eye_gaze.dto.EyeGazeAnalyzeResponse;
import com.example.interviewfitbe.domain.eye_gaze.entity.EyeGazeReport;
import com.example.interviewfitbe.domain.eye_gaze.repository.EyeGazeReportRepository;
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
public class EyeGazeService {

    private final EyeGazeReportRepository eyeGazeReportRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper;
    private final EyeGazeAnalysisService eyeGazeAnalysisService;

    @Value("${app.upload-dir}")
    private String uploadDir;

    @Transactional
    public EyeGazeAnalyzeResponse analyzeAndSave(
            String videoObjectKey,
            InterviewEntity interview
    ) {
        validateVideoUrl(videoObjectKey);

        Map<String, Object> fastApiResponse =
                eyeGazeAnalysisService.analyzeGaze(videoObjectKey);

        Map<String, Object> analysisResult =
                extractAnalysisResult(fastApiResponse);

        EyeGazeReport report = EyeGazeReport.builder()
                .interview(interview)
                .videoUrl(videoObjectKey)
                .centerRatio(
                        getDouble(
                                analysisResult,
                                "center_ratio"
                        )
                )
                .gazeAwayRatio(
                        getDouble(
                                analysisResult,
                                "gaze_away_ratio"
                        )
                )
                .gazeAwayCount(
                        getInteger(
                                analysisResult,
                                "gaze_away_count"
                        )
                )
                .score(
                        getDouble(
                                analysisResult,
                                "score"
                        )
                )
                .evaluation(
                        getString(
                                analysisResult,
                                "evaluation"
                        )
                )
                .resultJson(
                        toJson(fastApiResponse)
                )
                .build();

        EyeGazeReport saved =
                eyeGazeReportRepository.save(report);

        return new EyeGazeAnalyzeResponse(
                "시선 분석 및 저장 완료",
                saved.getId(),
                analysisResult
        );
    }

    @Transactional
    public EyeGazeAnalyzeResponse analyzeAndSave(
            EyeGazeAnalyzeRequest request
    ) {
        validateVideoUrl(request.getVideoUrl());

        InterviewEntity interview =
                findInterviewOrNull(
                        request.getInterviewId()
                );

        Map<String, Object> fastApiResponse =
                eyeGazeAnalysisService.analyzeGaze(
                        request.getVideoUrl()
                );

        Map<String, Object> analysisResult =
                extractAnalysisResult(fastApiResponse);

        EyeGazeReport report = EyeGazeReport.builder()
                .interview(interview)
                .videoUrl(request.getVideoUrl())
                .centerRatio(
                        getDouble(
                                analysisResult,
                                "center_ratio"
                        )
                )
                .gazeAwayRatio(
                        getDouble(
                                analysisResult,
                                "gaze_away_ratio"
                        )
                )
                .gazeAwayCount(
                        getInteger(
                                analysisResult,
                                "gaze_away_count"
                        )
                )
                .score(
                        getDouble(
                                analysisResult,
                                "score"
                        )
                )
                .evaluation(
                        getString(
                                analysisResult,
                                "evaluation"
                        )
                )
                .resultJson(
                        toJson(fastApiResponse)
                )
                .build();

        EyeGazeReport saved =
                eyeGazeReportRepository.save(report);

        return new EyeGazeAnalyzeResponse(
                "시선 분석 및 저장 완료",
                saved.getId(),
                analysisResult
        );
    }

    @Transactional
    public EyeGazeAnalyzeResponse analyzeAndSave(
            String videoObjectKey,
            InterviewAnswerEntity interviewAnswer
    ) {
        validateVideoUrl(videoObjectKey);

        Map<String, Object> fastApiResponse =
                eyeGazeAnalysisService.analyzeGaze(
                        videoObjectKey
                );

        Map<String, Object> analysisResult =
                extractAnalysisResult(fastApiResponse);

        EyeGazeReport report = EyeGazeReport.builder()
                .interview(
                        interviewAnswer.getInterview()
                )
                .interviewAnswer(interviewAnswer)
                .videoUrl(videoObjectKey)
                .centerRatio(
                        getDouble(
                                analysisResult,
                                "center_ratio"
                        )
                )
                .gazeAwayRatio(
                        getDouble(
                                analysisResult,
                                "gaze_away_ratio"
                        )
                )
                .gazeAwayCount(
                        getInteger(
                                analysisResult,
                                "gaze_away_count"
                        )
                )
                .score(
                        getDouble(
                                analysisResult,
                                "score"
                        )
                )
                .evaluation(
                        getString(
                                analysisResult,
                                "evaluation"
                        )
                )
                .resultJson(
                        toJson(fastApiResponse)
                )
                .build();

        EyeGazeReport saved =
                eyeGazeReportRepository.save(report);

        return new EyeGazeAnalyzeResponse(
                "질문별 시선 분석 및 저장 완료",
                saved.getId(),
                analysisResult
        );
    }

    @Transactional
    public EyeGazeAnalyzeResponse analyzeUploadedVideoAndSave(
            MultipartFile file,
            Long userId,
            Long interviewId
    ) {
        String savedPath = saveVideoFile(file);

        EyeGazeAnalyzeRequest request =
                new EyeGazeAnalyzeRequest();

        request.setVideoUrl(savedPath);
        request.setUserId(userId);
        request.setInterviewId(interviewId);

        return analyzeAndSave(request);
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

    private InterviewEntity findInterviewOrNull(
            Long interviewId
    ) {
        if (interviewId == null) {
            return null;
        }

        return interviewRepository.findById(
                        interviewId
                )
                .orElseThrow(
                        () -> new EntityNotFoundException(
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
                fastApiResponse.get("result");

        if (result instanceof Map<?, ?>) {
            return (Map<String, Object>) result;
        }

        return fastApiResponse;
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

            File dir = new File(
                    uploadDir + "/eye-gaze"
            );

            if (!dir.exists()) {
                dir.mkdirs();
            }

            String originalFileName =
                    file.getOriginalFilename();

            String extension =
                    getExtension(originalFileName);

            String savedFileName =
                    UUID.randomUUID()
                            + extension;

            File savedFile =
                    new File(
                            dir,
                            savedFileName
                    );

            file.transferTo(savedFile);

            return savedFile.getAbsolutePath();

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
            return objectMapper.writeValueAsString(
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

        return ((Number) value)
                .intValue();
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

        return ((Number) value)
                .doubleValue();
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
            Object value = map.get(key);

            if (value != null) {
                return value;
            }
        }

        return null;
    }
}