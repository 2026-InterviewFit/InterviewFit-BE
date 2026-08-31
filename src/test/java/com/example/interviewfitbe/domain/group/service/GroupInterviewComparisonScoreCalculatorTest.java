package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupInterviewAnswerAnalysisEntity;
import com.example.interviewfitbe.domain.pose.calculator.PoseScoreCalculator;
import com.example.interviewfitbe.domain.pose.calculator.PoseStatisticsCalculator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class GroupInterviewComparisonScoreCalculatorTest {
    private final GroupInterviewComparisonScoreCalculator calculator =
            new GroupInterviewComparisonScoreCalculator(
                    new ObjectMapper(), new PoseStatisticsCalculator(), new PoseScoreCalculator());

    @Test
    void reusesExistingDomainScoresAndWeights() {
        GroupInterviewAnswerAnalysisEntity analysis = GroupInterviewAnswerAnalysisEntity.builder()
                .aiEvaluationJson("""
                        {"star_score":{"S":8,"T":9,"A":7,"R":6}}
                        """)
                .speechResultJson("""
                        {"result":{
                          "speech_score":82,
                          "filler_word_analysis":{"total_count":25},
                          "silence_analysis":{"pause_rate_percent":20,"critical_silence_count":4},
                          "speech_pace_analysis":{"characters_per_second":2.0}
                        }}
                        """)
                .speechScore(82)
                .eyeGazeResultJson("{" + "\"result\":{\"score\":90}}")
                .eyeBlinkingResultJson("{" + "\"result\":{\"score\":80}}")
                .poseResultJson("""
                        [
                          {"timestamp":1.0,"shoulder_status":"GOOD","face_status":"GOOD"},
                          {"timestamp":2.0,"shoulder_status":"GOOD","face_status":"GOOD"}
                        ]
                        """)
                .build();

        var scores = calculator.calculate(analysis);

        assertThat(scores.content()).isEqualByComparingTo(new BigDecimal("75.00"));
        assertThat(scores.speech()).isEqualByComparingTo(new BigDecimal("82.00"));
        assertThat(scores.gaze()).isEqualByComparingTo(new BigDecimal("90.00"));
        assertThat(scores.pose()).isEqualByComparingTo(new BigDecimal("100.00"));
        assertThat(scores.blink()).isEqualByComparingTo(new BigDecimal("80.00"));
        assertThat(scores.total()).isEqualByComparingTo(new BigDecimal("81.75"));
    }

    @Test
    void normalizesStarScoresFromTenPointScaleToOneHundredPointScale() {
        assertThat(contentScore(8, 7, 9, 6)).isEqualByComparingTo("75.00");
        assertThat(contentScore(10, 10, 10, 10)).isEqualByComparingTo("100.00");
        assertThat(contentScore(0, 0, 0, 0)).isEqualByComparingTo("0.00");
    }

    @Test
    void missingStarFieldProducesPartialResult() {
        GroupInterviewAnswerAnalysisEntity analysis = completeAnalysis(
                "{\"star_score\":{\"S\":8,\"T\":7,\"A\":9}}");

        var scores = calculator.calculate(analysis);

        assertThat(scores.content()).isNull();
        assertThat(scores.complete()).isFalse();
        assertThat(scores.total()).isNull();
    }

    @Test
    void starScoreAboveTenProducesPartialResult() {
        assertInvalidStar("{\"star_score\":{\"S\":11,\"T\":7,\"A\":9,\"R\":6}}");
    }

    @Test
    void negativeStarScoreProducesPartialResult() {
        assertInvalidStar("{\"star_score\":{\"S\":-1,\"T\":7,\"A\":9,\"R\":6}}");
    }

    @Test
    void missingDomainScoreDoesNotInventFallbackScore() {
        GroupInterviewAnswerAnalysisEntity analysis = GroupInterviewAnswerAnalysisEntity.builder()
                .aiEvaluationJson("{\"star_score\":{\"S\":8,\"T\":8,\"A\":8,\"R\":8}}")
                .speechResultJson("{}")
                .eyeGazeResultJson("{\"result\":{\"score\":90}}")
                .eyeBlinkingResultJson("{\"result\":{\"blink_rate\":15}}")
                .poseResultJson("[]")
                .build();

        var scores = calculator.calculate(analysis);

        assertThat(scores.complete()).isFalse();
        assertThat(scores.total()).isNull();
        assertThat(scores.blink()).isNull();
    }

    private BigDecimal contentScore(int s, int t, int a, int r) {
        return calculator.calculate(completeAnalysis(String.format(
                "{\"star_score\":{\"S\":%d,\"T\":%d,\"A\":%d,\"R\":%d}}",
                s, t, a, r))).content();
    }

    private void assertInvalidStar(String starJson) {
        var scores = calculator.calculate(completeAnalysis(starJson));
        assertThat(scores.content()).isNull();
        assertThat(scores.complete()).isFalse();
        assertThat(scores.total()).isNull();
    }

    private GroupInterviewAnswerAnalysisEntity completeAnalysis(String starJson) {
        return GroupInterviewAnswerAnalysisEntity.builder()
                .aiEvaluationJson(starJson)
                .speechResultJson("{}")
                .eyeGazeResultJson("{\"result\":{\"score\":90}}")
                .eyeBlinkingResultJson("{\"result\":{\"score\":80}}")
                .poseResultJson("""
                        [
                          {"timestamp":1.0,"shoulder_status":"GOOD","face_status":"GOOD"},
                          {"timestamp":2.0,"shoulder_status":"GOOD","face_status":"GOOD"}
                        ]
                        """)
                .build();
    }
}
