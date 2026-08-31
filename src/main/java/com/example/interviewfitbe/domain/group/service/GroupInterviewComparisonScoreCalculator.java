package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerAnalysisEntity;
import com.example.interviewfitbe.domain.pose.calculator.PoseScoreCalculator;
import com.example.interviewfitbe.domain.pose.calculator.PoseStatisticsCalculator;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResponse;
import com.example.interviewfitbe.domain.pose.entity.PoseBadLogEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseNotDetectedLogEntity;
import com.example.interviewfitbe.domain.pose.enums.PoseIssueType;
import com.example.interviewfitbe.domain.pose.enums.PoseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
@RequiredArgsConstructor
public class GroupInterviewComparisonScoreCalculator {
    private static final BigDecimal CONTENT_WEIGHT = new BigDecimal("0.25");
    private static final BigDecimal SPEECH_WEIGHT = new BigDecimal("0.20");
    private static final BigDecimal GAZE_WEIGHT = new BigDecimal("0.20");
    private static final BigDecimal POSE_WEIGHT = new BigDecimal("0.20");
    private static final BigDecimal BLINK_WEIGHT = new BigDecimal("0.15");
    private static final BigDecimal STAR_MAX_SCORE = new BigDecimal("10");
    private static final BigDecimal STAR_NORMALIZATION_FACTOR = new BigDecimal("10");

    private final ObjectMapper objectMapper;
    private final PoseStatisticsCalculator poseStatisticsCalculator;
    private final PoseScoreCalculator poseScoreCalculator;

    public Scores calculate(GroupInterviewAnswerAnalysisEntity analysis) {
        BigDecimal content = contentScore(analysis.getAiEvaluationJson());
        BigDecimal speech = analysis.getSpeechScore() == null
                ? null
                : round(BigDecimal.valueOf(analysis.getSpeechScore()));
        BigDecimal gaze = resultScore(analysis.getEyeGazeResultJson());
        BigDecimal pose = poseScore(analysis.getPoseResultJson());
        BigDecimal blink = resultScore(analysis.getEyeBlinkingResultJson());
        BigDecimal total = allPresent(content, speech, gaze, pose, blink)
                ? round(content.multiply(CONTENT_WEIGHT)
                .add(speech.multiply(SPEECH_WEIGHT))
                .add(gaze.multiply(GAZE_WEIGHT))
                .add(pose.multiply(POSE_WEIGHT))
                .add(blink.multiply(BLINK_WEIGHT)))
                : null;
        return new Scores(content, speech, gaze, pose, blink, total);
    }

    private BigDecimal contentScore(String json) {
        JsonNode root = tree(json);
        if (root == null) return null;
        JsonNode star = first(root, "star_score", "starScore");
        if (star == null || !star.isObject()) return null;
        List<BigDecimal> values = new ArrayList<>();
        for (String key : List.of("s", "t", "a", "r")) {
            JsonNode value = fieldIgnoreCase(star, key);
            if (value == null || !value.isNumber()) return null;
            BigDecimal score = value.decimalValue();
            if (score.compareTo(BigDecimal.ZERO) < 0 || score.compareTo(STAR_MAX_SCORE) > 0) {
                return null;
            }
            values.add(score);
        }
        return clamp(average(values).multiply(STAR_NORMALIZATION_FACTOR))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal resultScore(String json) {
        JsonNode root = result(tree(json));
        JsonNode score = root == null ? null : first(root, "score");
        return score != null && score.isNumber() ? round(clamp(score.decimalValue())) : null;
    }

    private BigDecimal poseScore(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            List<PoseAnalysisResponse> responses = objectMapper.readValue(
                    json, new TypeReference<List<PoseAnalysisResponse>>() {});
            if (responses.isEmpty() || responses.get(responses.size() - 1).getTimestamp() == null) return null;
            double totalDuration = responses.get(responses.size() - 1).getTimestamp();
            List<PoseBadLogEntity> bad = new ArrayList<>();
            List<PoseNotDetectedLogEntity> notDetected = new ArrayList<>();
            for (PoseAnalysisResponse response : responses) {
                addPoseLog(response.getTimestamp(), response.getShoulderStatus(), PoseIssueType.SHOULDER,
                        response.getShoulderTilt(), bad, notDetected);
                addPoseLog(response.getTimestamp(), response.getFaceStatus(), PoseIssueType.FACE,
                        response.getFaceDirection(), bad, notDetected);
            }
            double badDuration = poseStatisticsCalculator.calculateStatistics(bad).getBadDuration();
            double missingDuration = poseStatisticsCalculator
                    .calculateNotDetectedStatistics(notDetected).getNotDetectedDuration();
            return BigDecimal.valueOf(poseScoreCalculator
                    .calculateScoreResult(badDuration, missingDuration, totalDuration).getScore())
                    .setScale(2, RoundingMode.HALF_UP);
        } catch (Exception exception) {
            return null;
        }
    }

    private void addPoseLog(Double timestamp, String status, PoseIssueType type, Double deviation,
                            List<PoseBadLogEntity> bad,
                            List<PoseNotDetectedLogEntity> notDetected) {
        if (timestamp == null || status == null) return;
        if (PoseStatus.BAD.name().equals(status)) {
            bad.add(PoseBadLogEntity.builder().timestamp(timestamp).issueType(type).deviation(deviation).build());
        } else if (PoseStatus.NOT_DETECTED.name().equals(status)) {
            notDetected.add(PoseNotDetectedLogEntity.builder().timestamp(timestamp).issueType(type).build());
        }
    }

    private JsonNode tree(String json) {
        if (json == null || json.isBlank()) return null;
        try { return objectMapper.readTree(json); } catch (Exception exception) { return null; }
    }

    private JsonNode result(JsonNode node) {
        if (node == null) return null;
        JsonNode result = node.get("result");
        return result != null && result.isObject() ? result : node;
    }

    private JsonNode first(JsonNode node, String... keys) {
        for (String key : keys) {
            JsonNode value = node.get(key);
            if (value != null && !value.isNull()) return value;
        }
        return null;
    }

    private JsonNode fieldIgnoreCase(JsonNode node, String key) {
        Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
        while (fields.hasNext()) {
            var field = fields.next();
            if (field.getKey().equalsIgnoreCase(key)) return field.getValue();
        }
        return null;
    }

    private Integer integerAt(JsonNode root, String parent, String child) {
        JsonNode value = root.path(parent).get(child);
        return value != null && value.isNumber() ? value.intValue() : null;
    }

    private Double doubleAt(JsonNode root, String parent, String child) {
        JsonNode value = root.path(parent).get(child);
        return value != null && value.isNumber() ? value.doubleValue() : null;
    }

    private boolean allPresent(Object... values) { return Arrays.stream(values).allMatch(Objects::nonNull); }
    private BigDecimal clamp(BigDecimal value) {
        return value.max(BigDecimal.ZERO).min(new BigDecimal("100"));
    }
    private BigDecimal average(List<BigDecimal> values) {
        return round(values.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 6, RoundingMode.HALF_UP));
    }
    private BigDecimal round(BigDecimal value) { return value.setScale(2, RoundingMode.HALF_UP); }

    public record Scores(BigDecimal content, BigDecimal speech, BigDecimal gaze,
                         BigDecimal pose, BigDecimal blink, BigDecimal total) {
        public boolean complete() { return total != null; }
    }
}
