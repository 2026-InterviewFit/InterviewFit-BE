package com.example.interviewfitbe.domain.community.answer.service;

import com.example.interviewfitbe.domain.community.answer.dto.CommunityBestAnswerPublishRequest;
import com.example.interviewfitbe.domain.community.answer.dto.CommunityBestAnswerResponse;
import com.example.interviewfitbe.domain.community.answer.entity.CommunityBestAnswerEntity;
import com.example.interviewfitbe.domain.community.answer.repository.CommunityBestAnswerRepository;
import com.example.interviewfitbe.domain.eye_blinking.repository.EyeBlinkingReportRepository;
import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.interview.repository.InterviewAnswerRepository;
import com.example.interviewfitbe.domain.pose.repository.PoseReportRepository;
import com.example.interviewfitbe.domain.speech_coaching.entity.AiEvaluationEntity;
import com.example.interviewfitbe.domain.speech_coaching.entity.SpeechReportEntity;
import com.example.interviewfitbe.domain.speech_coaching.repository.AiEvaluationRepository;
import com.example.interviewfitbe.domain.speech_coaching.repository.SpeechReportRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommunityBestAnswerService {

    public static final int MINIMUM_PUBLISH_SCORE = 80;

    private final CommunityBestAnswerRepository bestAnswerRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final EyeBlinkingReportRepository eyeBlinkingReportRepository;
    private final PoseReportRepository poseReportRepository;
    private final SpeechReportRepository speechReportRepository;
    private final AiEvaluationRepository aiEvaluationRepository;
    private final ObjectMapper objectMapper;

    /**
     * 공개 우수 답변 목록.
     */
    @Transactional(readOnly = true)
    public List<CommunityBestAnswerResponse> getBestAnswers() {
        return bestAnswerRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(CommunityBestAnswerResponse::from)
                .toList();
    }

    /**
     * 본인의 분석 완료 답변을 우수 답변 피드에 공개한다.
     *
     * 서버에서 소유권과 80점 기준을 다시 검증하므로
     * FE가 임의 점수를 보내도 공개할 수 없다.
     */
    @Transactional
    public CommunityBestAnswerResponse publish(
            Long userId,
            CommunityBestAnswerPublishRequest request
    ) {
        if (request == null || request.getAnswerId() == null) {
            throw new IllegalArgumentException(
                    "공개할 answerId가 필요합니다."
            );
        }

        Long answerId = request.getAnswerId();

        if (bestAnswerRepository.existsBySourceAnswerId(answerId)) {
            throw new IllegalStateException(
                    "이미 우수 답변 피드에 공개된 답변입니다."
            );
        }

        InterviewAnswerEntity answer = interviewAnswerRepository
                .findWithInterviewById(answerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "답변을 찾을 수 없습니다. answerId=" + answerId
                        )
                );

        InterviewEntity interview = answer.getInterviewSafely();

        if (interview == null) {
            throw new IllegalStateException(
                    "답변에 연결된 면접 정보를 찾을 수 없습니다."
            );
        }

        if (!interview.getUser().getId().equals(userId)) {
            throw new AccessDeniedException(
                    "본인의 면접 답변만 공개할 수 있습니다."
            );
        }

        if (answer.getAnalysisStatus() != AnalysisStatus.DONE) {
            throw new IllegalStateException(
                    "AI 분석이 완료된 답변만 공개할 수 있습니다."
            );
        }

        if (interview.getJobRole() == null) {
            throw new IllegalStateException(
                    "지원 직무 정보가 없어 답변을 공개할 수 없습니다."
            );
        }

        SpeechReportEntity speechReport = speechReportRepository
                .findByInterview_Id(interview.getId())
                .orElse(null);

        AiEvaluationEntity aiEvaluation = aiEvaluationRepository
                .findByInterviewAnswer_Id(answerId)
                .orElse(null);

        int score = calculateOverallScore(
                interview.getId(),
                speechReport,
                aiEvaluation
        );

        if (score < MINIMUM_PUBLISH_SCORE) {
            throw new IllegalStateException(
                    "종합 80점 이상의 답변만 공개할 수 있습니다. 현재 점수="
                            + score
            );
        }

        String script = resolveScript(
                answer,
                speechReport
        );

        if (script == null || script.isBlank()) {
            throw new IllegalStateException(
                    "공개할 STT 답변 내용이 없습니다."
            );
        }

        String companyName = normalizeCompanyName(
                request.getCompanyName()
        );

        CommunityBestAnswerEntity saved = bestAnswerRepository.save(
                CommunityBestAnswerEntity.builder()
                        .user(interview.getUser())
                        .jobRole(interview.getJobRole())
                        .sourceAnswerId(answerId)
                        .sourceInterviewId(interview.getId())
                        .companyName(companyName)
                        .score(score)
                        .answerScript(script.trim())
                        .likeCount(0)
                        .build()
        );

        return CommunityBestAnswerResponse.from(saved);
    }

    /**
     * dashboard와 동일하게 시선/자세/답변 축을 최대한 활용하되,
     * 현재 BE에 확실히 저장되는 점수만 사용한다.
     *
     * 답변 축 = Speech 전달 점수와 Gemini STAR 내용 점수의 평균.
     * Gemini STAR는 각 S/T/A/R이 0~10이므로 평균 * 10으로 100점 환산.
     */
    private int calculateOverallScore(
            Long interviewId,
            SpeechReportEntity speechReport,
            AiEvaluationEntity aiEvaluation
    ) {
        List<Double> overallParts = new ArrayList<>();

        eyeBlinkingReportRepository
                .findTopByInterview_IdOrderByCreatedAtDesc(interviewId)
                .map(report -> report.getScore())
                .filter(this::isFiniteScore)
                .ifPresent(overallParts::add);

        poseReportRepository
                .findTopByInterview_IdOrderByIdDesc(interviewId)
                .map(report -> report.getScore() == null
                        ? null
                        : report.getScore().doubleValue())
                .filter(this::isFiniteScore)
                .ifPresent(overallParts::add);

        List<Double> speechParts = new ArrayList<>();

        if (speechReport != null && isFiniteScore(speechReport.getScore())) {
            speechParts.add(speechReport.getScore());
        }

        Double contentScore = calculateStarContentScore(aiEvaluation);
        if (isFiniteScore(contentScore)) {
            speechParts.add(contentScore);
        }

        Double speechCombined = average(speechParts);
        if (isFiniteScore(speechCombined)) {
            overallParts.add(speechCombined);
        }

        Double overall = average(overallParts);

        if (!isFiniteScore(overall)) {
            throw new IllegalStateException(
                    "우수 답변 공개에 필요한 분석 점수를 찾을 수 없습니다."
            );
        }

        return (int) Math.round(
                Math.max(0.0, Math.min(100.0, overall))
        );
    }

    private Double calculateStarContentScore(
            AiEvaluationEntity aiEvaluation
    ) {
        if (aiEvaluation == null
                || aiEvaluation.getStarEvaluationJson() == null
                || aiEvaluation.getStarEvaluationJson().isBlank()) {
            return null;
        }

        try {
            JsonNode root = objectMapper.readTree(
                    aiEvaluation.getStarEvaluationJson()
            );

            if (!root.isObject()) {
                return null;
            }

            double sum = 0.0;
            int count = 0;

            Iterator<Map.Entry<String, JsonNode>> fields = root.fields();

            while (fields.hasNext()) {
                JsonNode value = fields.next().getValue();

                if (value != null && value.isNumber()) {
                    double score = value.asDouble();

                    if (score >= 0.0 && score <= 10.0) {
                        sum += score;
                        count++;
                    }
                }
            }

            if (count == 0) {
                return null;
            }

            return (sum / count) * 10.0;

        } catch (Exception ignored) {
            return null;
        }
    }

    private String resolveScript(
            InterviewAnswerEntity answer,
            SpeechReportEntity speechReport
    ) {
        if (answer.getAnswerText() != null
                && !answer.getAnswerText().isBlank()) {
            return answer.getAnswerText();
        }

        if (speechReport != null
                && speechReport.getTextScript() != null
                && !speechReport.getTextScript().isBlank()) {
            return speechReport.getTextScript();
        }

        return null;
    }

    private String normalizeCompanyName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return "기업 비공개";
        }

        String trimmed = companyName.trim();

        if (trimmed.length() > 100) {
            throw new IllegalArgumentException(
                    "기업명은 100자 이하로 입력해주세요."
            );
        }

        return trimmed;
    }

    private boolean isFiniteScore(Double score) {
        return score != null
                && Double.isFinite(score);
    }

    private Double average(List<Double> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }

        return values.stream()
                .filter(this::isFiniteScore)
                .mapToDouble(Double::doubleValue)
                .average()
                .orElse(Double.NaN);
    }
}
