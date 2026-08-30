package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.eye_blinking.service.EyeBlinkingAnalysisService;
import com.example.interviewfitbe.domain.eye_gaze.service.EyeGazeAnalysisService;
import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerAnalysisEntity;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerAnalysisRepository;
import com.example.interviewfitbe.domain.group.repository.GroupQuestionRepository;
import com.example.interviewfitbe.domain.job.repository.JobRoleRepository;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResponse;
import com.example.interviewfitbe.domain.pose.service.PoseAnalysisService;
import com.example.interviewfitbe.domain.speech_coaching.service.SpeechAnalysisService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroupInterviewAnswerAnalysisJobService {

    private final GroupInterviewAnswerAnalysisRepository analysisRepository;
    private final GroupQuestionRepository questionRepository;
    private final JobRoleRepository jobRoleRepository;
    private final EyeBlinkingAnalysisService eyeBlinkingAnalysisService;
    private final EyeGazeAnalysisService eyeGazeAnalysisService;
    private final PoseAnalysisService poseAnalysisService;
    private final SpeechAnalysisService speechAnalysisService;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Autowired(required = false)
    private GroupInterviewComparisonService comparisonService;

    @Async("analysisExecutor")
    public void analyzeAsync(Long answerId) {
        log.info("그룹 답변 분석 시작: answerId={}", answerId);
        try {
            AnalysisContext context = markRunningAndLoadContext(answerId);

            Map<String, Object> speech = speechAnalysisService.analyzeSpeech(
                    "minio",
                    context.videoObjectKey(),
                    context.jobCategory(),
                    context.roomId(),
                    context.answerId(),
                    context.questionId(),
                    context.questionText()
            );
            Map<String, Object> blinking =
                    eyeBlinkingAnalysisService.analyzeBlinking(context.videoObjectKey());
            Map<String, Object> gaze =
                    eyeGazeAnalysisService.analyzeGaze(context.videoObjectKey());
            List<PoseAnalysisResponse> pose =
                    poseAnalysisService.analyzePosture(context.videoObjectKey());

            Map<String, Object> speechPayload = extractResult(speech);
            saveDone(
                    answerId,
                    stringValue(speechPayload, "text_script", "textScript"),
                    toJson(speech),
                    toJson(speechPayload.get("ai_evaluation")),
                    toJson(speechPayload.get("external_ai_analysis")),
                    toJson(blinking),
                    toJson(gaze),
                    toJson(pose)
            );
            triggerComparison(context.roomId());
            log.info("그룹 답변 분석 완료: answerId={}", answerId);
        } catch (AnalysisAlreadyClaimedException exception) {
            log.info("이미 실행 또는 완료된 그룹 답변 분석을 건너뜁니다. answerId={}", answerId);
        } catch (Exception exception) {
            log.error("그룹 답변 분석 실패: answerId={}", answerId, exception);
            markFailed(answerId, exception.getMessage());
        }
    }

    private AnalysisContext markRunningAndLoadContext(Long answerId) {
        return transactionTemplate.execute(status -> {
            GroupInterviewAnswerAnalysisEntity analysis = analysisRepository
                    .findByAnswerIdForUpdate(answerId)
                    .orElseThrow(() -> new IllegalArgumentException(
                            "그룹 답변 분석 정보를 찾을 수 없습니다. answerId=" + answerId
                    ));
            if (analysis.getStatus() == AnalysisStatus.RUNNING
                    || analysis.getStatus() == AnalysisStatus.DONE) {
                throw new AnalysisAlreadyClaimedException();
            }
            var answer = analysis.getAnswer();
            if (!answer.isVideoUploaded()) {
                throw new IllegalStateException("분석할 답변 영상이 없습니다.");
            }

            String questionText = answer.getQuestionId() == null
                    ? null
                    : questionRepository.findById(answer.getQuestionId())
                    .map(question -> question.getQuestionText())
                    .orElse(null);
            String jobCategory = jobRoleRepository
                    .findById(answer.getGroupInterview().getJobRoleId())
                    .map(jobRole -> jobRole.getCategory())
                    .orElse(null);

            analysis.markRunning(Instant.now());
            analysisRepository.saveAndFlush(analysis);
            return new AnalysisContext(
                    answer.getId(),
                    answer.getGroupInterview().getId(),
                    answer.getQuestionId(),
                    questionText,
                    jobCategory,
                    answer.getVideoObjectKey()
            );
        });
    }

    private void saveDone(
            Long answerId,
            String speechText,
            String speechJson,
            String aiEvaluationJson,
            String externalAiAnalysisJson,
            String blinkingJson,
            String gazeJson,
            String poseJson
    ) {
        transactionTemplate.executeWithoutResult(status -> {
            GroupInterviewAnswerAnalysisEntity analysis = requiredAnalysis(answerId);
            analysis.markDone(
                    speechText,
                    speechJson,
                    aiEvaluationJson,
                    externalAiAnalysisJson,
                    blinkingJson,
                    gazeJson,
                    poseJson,
                    Instant.now()
            );
            analysisRepository.save(analysis);
        });
    }

    private void markFailed(Long answerId, String errorMessage) {
        try {
            transactionTemplate.executeWithoutResult(status -> {
                GroupInterviewAnswerAnalysisEntity analysis = requiredAnalysis(answerId);
                analysis.markFailed(errorMessage, Instant.now());
                analysisRepository.save(analysis);
            });
            analysisRepository.findByAnswerId(answerId).ifPresent(analysis ->
                    triggerComparison(analysis.getAnswer().getGroupInterview().getId()));
        } catch (Exception persistenceException) {
            log.error("그룹 답변 분석 실패 상태 저장 실패: answerId={}", answerId, persistenceException);
        }
    }

    private void triggerComparison(Long roomId) {
        if (comparisonService != null) {
            try {
                comparisonService.generateAvailableComparisons(roomId);
            } catch (Exception exception) {
                log.warn("그룹 상대 비교 생성은 다음 시도로 연기합니다. roomId={}, error={}",
                        roomId, exception.getMessage());
            }
        }
    }

    private GroupInterviewAnswerAnalysisEntity requiredAnalysis(Long answerId) {
        return analysisRepository.findByAnswerId(answerId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "그룹 답변 분석 정보를 찾을 수 없습니다. answerId=" + answerId
                ));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractResult(Map<String, Object> response) {
        Object result = response.get("result");
        return result instanceof Map<?, ?> ? (Map<String, Object>) result : response;
    }

    private String stringValue(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value != null) {
                return value.toString();
            }
        }
        return null;
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception exception) {
            throw new IllegalStateException("분석 결과 JSON 변환에 실패했습니다.", exception);
        }
    }

    private record AnalysisContext(
            Long answerId,
            Long roomId,
            Long questionId,
            String questionText,
            String jobCategory,
            String videoObjectKey
    ) {
    }

    private static class AnalysisAlreadyClaimedException extends RuntimeException {
    }
}
