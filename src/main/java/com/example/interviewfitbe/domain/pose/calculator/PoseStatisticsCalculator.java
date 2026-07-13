package com.example.interviewfitbe.domain.pose.calculator;

import com.example.interviewfitbe.domain.pose.dto.statistics.PoseNotDetectedStatistics;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseStatistics;
import com.example.interviewfitbe.domain.pose.entity.PoseBadLogEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseNotDetectedLogEntity;
import com.example.interviewfitbe.domain.pose.enums.PoseIssueType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PoseStatisticsCalculator {
    private static final double TIMESTAMP_EPSILON = 0.000001;
    private static final double TIMESTAMP_GAP = 1.000001;

    public PoseStatistics calculateStatistics(List<PoseBadLogEntity> logs) {
        List<Double> allTimestamps = extractAllBadTimestamps(logs);
        List<Double> shoulderTimestamps = extractBadTimestamps(logs, PoseIssueType.SHOULDER);
        List<Double> faceTimestamps = extractBadTimestamps(logs, PoseIssueType.FACE);

        return PoseStatistics.builder()
                .badDuration(calculateDuration(allTimestamps))
                .shoulderBadDuration(calculateDuration(shoulderTimestamps))
                .faceBadDuration(calculateDuration(faceTimestamps))
                .badCount(calculateCount(allTimestamps))
                .shoulderBadCount(calculateCount(shoulderTimestamps))
                .faceBadCount(calculateCount(faceTimestamps))
                .build();
    }

    public PoseNotDetectedStatistics calculateNotDetectedStatistics(List<PoseNotDetectedLogEntity> logs) {
        List<Double> allTimestamps = extractAllNotDetectedTimestamps(logs);
        List<Double> shoulderTimestamps = extractNotDetectedTimestamps(logs, PoseIssueType.SHOULDER);
        List<Double> faceTimestamps = extractNotDetectedTimestamps(logs, PoseIssueType.FACE);

        return PoseNotDetectedStatistics.builder()
                .notDetectedDuration(calculateDuration(allTimestamps))
                .shoulderNotDetectedDuration(calculateDuration(shoulderTimestamps))
                .faceNotDetectedDuration(calculateDuration(faceTimestamps))
                .build();
    }

    public double calculateDuration(List<Double> timestamps) {
        if (timestamps.isEmpty()) {
            return 0.0;
        }

        double lastTimestamp = timestamps.get(timestamps.size() - 1);
        double decimal = lastTimestamp % 1;

        double lastDuration = Math.abs(decimal) < TIMESTAMP_EPSILON
                ? 1.0
                : decimal;

        return (timestamps.size() - 1) + lastDuration;
    }

    public int calculateCount(List<Double> timestamps) {
        if (timestamps.isEmpty()) {
            return 0;
        }

        int count = 1;
        for (int i = 1; i < timestamps.size(); i++) {
            double gap = timestamps.get(i) - timestamps.get(i - 1);
            if (gap > TIMESTAMP_GAP) {
                count++;
            }
        }

        return count;
    }

    private List<Double> extractBadTimestamps(List<PoseBadLogEntity> logs, PoseIssueType type) {
        return logs.stream()
                .filter(log -> log.getIssueType() == type)
                .map(PoseBadLogEntity::getTimestamp)
                .distinct()
                .sorted()
                .toList();
    }

    private List<Double> extractNotDetectedTimestamps(List<PoseNotDetectedLogEntity> logs, PoseIssueType type) {
        return logs.stream()
                .filter(log -> log.getIssueType() == type)
                .map(PoseNotDetectedLogEntity::getTimestamp)
                .distinct()
                .sorted()
                .toList();
    }

    private List<Double> extractAllBadTimestamps(List<PoseBadLogEntity> logs) {
        return logs.stream()
                .map(PoseBadLogEntity::getTimestamp)
                .distinct()
                .sorted()
                .toList();
    }

    private List<Double> extractAllNotDetectedTimestamps(List<PoseNotDetectedLogEntity> logs) {
        return logs.stream()
                .map(PoseNotDetectedLogEntity::getTimestamp)
                .distinct()
                .sorted()
                .toList();
    }
}
