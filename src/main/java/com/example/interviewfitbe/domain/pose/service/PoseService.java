package com.example.interviewfitbe.domain.pose.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResponse;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseNotDetectedStatistics;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseScoreResult;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseStatistics;
import com.example.interviewfitbe.domain.pose.entity.PoseAnalysisResultEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseBadLogEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseNotDetectedLogEntity;
import com.example.interviewfitbe.domain.pose.enums.PoseGrade;
import com.example.interviewfitbe.domain.pose.enums.PoseIssueType;
import com.example.interviewfitbe.domain.pose.exception.PoseErrorCode;
import com.example.interviewfitbe.domain.pose.mapper.PoseMapper;
import com.example.interviewfitbe.domain.pose.repository.PoseAnalysisResultRepository;
import com.example.interviewfitbe.domain.pose.repository.PoseBadLogRepository;
import com.example.interviewfitbe.domain.pose.repository.PoseNotDetectedLogRepository;
import com.example.interviewfitbe.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PoseService {
    private final PoseAnalysisService poseAnalysisService;
    private final PoseBadLogRepository poseBadLogRepository;
    private final PoseNotDetectedLogRepository poseNotDetectedLogRepository;
    private final PoseAnalysisResultRepository poseAnalysisResultRepository;
    private final PoseMapper poseMapper;

    public List<PoseAnalysisResultDto> getAllPostureAnalysis() {
        return poseAnalysisResultRepository.findAll().stream()
                .map(poseMapper::toDto)
                .toList();
    }

    public PoseAnalysisResultDto getPoseAnalysis(Long id) {
        return poseMapper.toDto(
                poseAnalysisResultRepository.findById(id).orElseThrow(() ->
                        new BaseException(PoseErrorCode.POSE_ANALYSIS_RESULT_NOT_FOUND))
        );
    }

    public void analyzeFullPosture(String videoObjectKey, InterviewAnswerEntity interviewAnswer) {
        // 1. FastAPI 분석 결과 (전체 기준)
        List<PoseAnalysisResponse> responses = poseAnalysisService.analyzePosture(videoObjectKey);
        Double totalDuration = responses.isEmpty()
                ? 0.0
                : responses.get(responses.size() - 1).getTimestamp();

        if (totalDuration <= 0) {
            createEmptyBadLogResult(totalDuration, List.of(), interviewAnswer);
            return;
        }

        // 2. BAD 로그 및 Not Detected 로그 생성
        List<PoseBadLogEntity> badLogs = createBadLog(responses);
        List<PoseNotDetectedLogEntity> notDetectedLogs = createNotDetectedLog(responses);

        // 3. 분석
        if (badLogs.isEmpty()) {
            createEmptyBadLogResult(totalDuration, notDetectedLogs, interviewAnswer);
        } else {
            analyzePosture(badLogs, notDetectedLogs, totalDuration,interviewAnswer);
        }
    }

    private List<PoseBadLogEntity> createBadLog(List<PoseAnalysisResponse> responses) {
        List<PoseBadLogEntity> logs = new ArrayList<>();

        for (PoseAnalysisResponse analysis : responses) {
            if ("BAD".equals(analysis.getShoulderStatus())) {
                logs.add(PoseBadLogEntity.builder()
                        .timestamp(analysis.getTimestamp())
                        .issueType(PoseIssueType.SHOULDER)
                        .deviation(analysis.getShoulderTilt())
                        .build());
            }
            if ("BAD".equals(analysis.getFaceStatus())) {
                logs.add(PoseBadLogEntity.builder()
                        .timestamp(analysis.getTimestamp())
                        .issueType(PoseIssueType.FACE)
                        .deviation(analysis.getFaceDirection())
                        .build());
            }
        }

        return logs;
    }

    private List<PoseNotDetectedLogEntity> createNotDetectedLog(List<PoseAnalysisResponse> responses) {
        List<PoseNotDetectedLogEntity> logs = new ArrayList<>();

        for (PoseAnalysisResponse analysis : responses) {
            if ("NOT_DETECTED".equals(analysis.getShoulderStatus())) {
                logs.add(PoseNotDetectedLogEntity.builder()
                        .timestamp(analysis.getTimestamp())
                        .issueType(PoseIssueType.SHOULDER)
                        .build());
            }

            if ("NOT_DETECTED".equals(analysis.getFaceStatus())) {
                logs.add(PoseNotDetectedLogEntity.builder()
                        .timestamp(analysis.getTimestamp())
                        .issueType(PoseIssueType.FACE)
                        .build());
            }
        }

        return logs;
    }

    private void  createEmptyBadLogResult(Double totalDuration, List<PoseNotDetectedLogEntity> notDetectedLogs, InterviewAnswerEntity interviewAnswer) {
        PoseNotDetectedStatistics notDetectedStats = calculateNotDetectedStatistics(notDetectedLogs);
        PoseScoreResult result = calculateScoreResult(0.0, notDetectedStats.getNotDetectedDuration(), totalDuration);

        PoseAnalysisResultEntity saved = poseAnalysisResultRepository.save(
                poseMapper.toEmptyResult(totalDuration, notDetectedStats, result, interviewAnswer)
        );

        if (!notDetectedLogs.isEmpty()) {
            notDetectedLogs.forEach(entity -> entity.assignPoseAnalysisResult(saved));
            poseNotDetectedLogRepository.saveAll(notDetectedLogs);
        }
    }


    private void analyzePosture(List<PoseBadLogEntity> badLogs, List<PoseNotDetectedLogEntity> notDetectedLogs, Double totalDuration, InterviewAnswerEntity interviewAnswer) {
        PoseStatistics stats = calculateStatistics(badLogs);
        PoseNotDetectedStatistics notDetectedStats = calculateNotDetectedStatistics(notDetectedLogs);
        PoseScoreResult result = calculateScoreResult(stats.getBadDuration(), notDetectedStats.getNotDetectedDuration(), totalDuration);

        PoseAnalysisResultEntity saved = poseAnalysisResultRepository.save(
                poseMapper.toEntity(totalDuration, stats, notDetectedStats, result, interviewAnswer)
        );

        badLogs.forEach(entity -> entity.assignPoseAnalysisResult(saved));
        poseBadLogRepository.saveAll(badLogs);

        notDetectedLogs.forEach(entity -> entity.assignPoseAnalysisResult(saved));
        poseNotDetectedLogRepository.saveAll(notDetectedLogs);
    }

    private PoseStatistics calculateStatistics(List<PoseBadLogEntity> logs) {
        return PoseStatistics.builder()
                .badDuration(calculateDuration(logs.stream()
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .sorted()
                        .toList()))
                .shoulderBadDuration(calculateDuration(logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.SHOULDER)
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .sorted()
                        .toList()))
                .faceBadDuration(calculateDuration(logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.FACE)
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .sorted()
                        .toList()))
                .badCount(calculateCount(logs.stream()
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .sorted()
                        .toList()))
                .shoulderBadCount(calculateCount(logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.SHOULDER)
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .sorted()
                        .toList()))
                .faceBadCount(calculateCount(logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.FACE)
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .sorted()
                        .toList()))
                .build();
    }

    private PoseNotDetectedStatistics calculateNotDetectedStatistics(List<PoseNotDetectedLogEntity> logs) {
        return PoseNotDetectedStatistics.builder()
                .notDetectedDuration(calculateDuration(logs.stream()
                        .map(PoseNotDetectedLogEntity::getTimestamp)
                        .distinct()
                        .sorted()
                        .toList()))
                .shoulderNotDetectedDuration(calculateDuration(logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.SHOULDER)
                        .map(PoseNotDetectedLogEntity::getTimestamp)
                        .distinct()
                        .sorted()
                        .toList()))
                .faceNotDetectedDuration(calculateDuration(logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.FACE)
                        .map(PoseNotDetectedLogEntity::getTimestamp)
                        .distinct()
                        .sorted()
                        .toList()))
                .build();
    }

    private double calculateDuration(List<Double> timestamps) {
        if (timestamps.isEmpty()) {
            return 0.0;
        }

        double lastTimestamp = timestamps.get(timestamps.size() - 1);
        double decimal = lastTimestamp % 1;

        double lastDuration = Math.abs(decimal) < 0.000001
                ? 1.0
                : decimal;

        return (timestamps.size() - 1) + lastDuration;
    }

    private int calculateCount(List<Double> timestamps) {
        if (timestamps.isEmpty()) {
            return 0;
        }

        int count = 1;
        for (int i = 1; i < timestamps.size(); i++) {
            double gap = timestamps.get(i) - timestamps.get(i - 1);
            if (gap > 1.000001) {
                count++;
            }
        }

        return count;
    }

    private PoseScoreResult calculateScoreResult(double badDuration, double notDetectedDuration, double totalDuration) {
        double badRatio = Math.min(1.0, badDuration / totalDuration);
        int score = Math.max(0, 100 - (int) (badRatio * 100));

        double notDetectedRatio = notDetectedDuration / totalDuration;
        if (notDetectedRatio >= 0.3) {
            int penalty = (int) ((notDetectedRatio - 0.3) / 0.7 * 100);
            score = Math.max(0, score - penalty);
        }

        PoseGrade grade = calculateGrade(score);

        return PoseScoreResult.builder()
                .badRatio(badRatio)
                .score(score)
                .grade(grade)
                .build();
    }

    private PoseGrade calculateGrade(int score) {
        if (score >= 85) return PoseGrade.A;
        else if (score >= 75) return PoseGrade.B;
        else if (score >= 65) return PoseGrade.C;
        else if (score >= 50) return PoseGrade.D;
        else return PoseGrade.E;
    }
}