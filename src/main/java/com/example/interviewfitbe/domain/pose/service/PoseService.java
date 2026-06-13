package com.example.interviewfitbe.domain.pose.service;

import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResponse;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.dto.PoseRequest;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseScoreResult;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseStatistics;
import com.example.interviewfitbe.domain.pose.entity.PoseAnalysisResultEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseBadLogEntity;
import com.example.interviewfitbe.domain.pose.enums.PoseGrade;
import com.example.interviewfitbe.domain.pose.enums.PoseIssueType;
import com.example.interviewfitbe.domain.pose.mapper.PoseMapper;
import com.example.interviewfitbe.domain.pose.repository.PoseAnalysisResultRepository;
import com.example.interviewfitbe.domain.pose.repository.PoseBadLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class PoseService {
    private final PoseAnalysisService poseAnalysisService;
    private final PoseBadLogRepository poseBadLogRepository;
    private final PoseAnalysisResultRepository poseAnalysisResultRepository;
    private final PoseMapper poseMapper;

    public PoseAnalysisResultDto analyzeFullPosture(PoseRequest request) {
        // 1. FastAPI 분석 결과 (전체 기준)
        List<PoseAnalysisResponse> responses = this.poseAnalysisService.analyzePosture(request.getVideoObjectKey());
        Integer totalDuration = responses.size();

        // 2. BAD 로그 생성 + 저장
        List<PoseBadLogEntity> logs = saveBadLog(responses);

        // 3. 분석
        if (logs.isEmpty()) {
            return createEmptyBadLogResult(totalDuration);
        }
        return analyzePosture(logs, totalDuration);
    }

    private List<PoseBadLogEntity> saveBadLog(List<PoseAnalysisResponse> responses) {
        List<PoseBadLogEntity> logs = new ArrayList<>();

        for (PoseAnalysisResponse analysis : responses) {
            // 어깨 BAD 및 NOT_DETECTED 로그 저장
            if ("ShoulderStatus.BAD".equals(analysis.getShoulderStatus())) {
                logs.add(PoseBadLogEntity.builder()
                        .timestamp(analysis.getTimestamp())
                        .issueType(PoseIssueType.SHOULDER)
                        .deviation(analysis.getShoulderTilt())
                        .build());
            }

            // 얼굴 BAD 및 NOT_DETECTED 로그 저장
            if ("FaceStatus.BAD".equals(analysis.getFaceStatus())) {
                logs.add(PoseBadLogEntity.builder()
                        .timestamp(analysis.getTimestamp())
                        .issueType(PoseIssueType.FACE)
                        .deviation(analysis.getFaceDirection())
                        .build());
            }
        }

        return this.poseBadLogRepository.saveAll(logs);
    }

    private PoseAnalysisResultDto createEmptyBadLogResult(Integer totalDuration) {
        PoseAnalysisResultEntity result = createEmptyResult(totalDuration);

        return this.poseMapper.toDto(this.poseAnalysisResultRepository.save(result));
    }

    private PoseAnalysisResultDto analyzePosture(List<PoseBadLogEntity> logs, Integer totalDuration) {
        // 1. 각 타입 별 집계 수행
        PoseStatistics stats = calculateStatistics(logs);

        // 2. 점수화
        PoseScoreResult result = calculateScoreResult(stats.getBadDuration(), totalDuration);

        PoseAnalysisResultEntity entity = this.poseMapper.toEntity(totalDuration, stats, result);

        return this.poseMapper.toDto(this.poseAnalysisResultRepository.save(entity));
    }

    private PoseStatistics calculateStatistics(List<PoseBadLogEntity> logs) {

        PoseStatistics stats = new PoseStatistics();

        // 전체 BAD 시간
        stats.setBadDuration(
                (int) logs.stream()
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .count()
        );

        // 어깨 BAD 시간
        stats.setShoulderBadDuration(
                (int) logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.SHOULDER)
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .count()
        );

        // 얼굴 BAD 시간
        stats.setFaceBadDuration(
                (int) logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.FACE)
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .count()
        );

        // 어깨 BAD 발생 횟수
        stats.setShoulderBadCount(
                calculateBadCount(
                        logs.stream()
                                .filter(log -> log.getIssueType() == PoseIssueType.SHOULDER)
                                .map(PoseBadLogEntity::getTimestamp)
                                .distinct()
                                .sorted()
                                .toList()
                )
        );

        // 얼굴 BAD 발생 횟수
        stats.setFaceBadCount(
                calculateBadCount(
                        logs.stream()
                                .filter(log -> log.getIssueType() == PoseIssueType.FACE)
                                .map(PoseBadLogEntity::getTimestamp)
                                .distinct()
                                .sorted()
                                .toList()
                )
        );

        // 전체 BAD 발생 횟수
        stats.setBadCount(
                calculateBadCount(
                        logs.stream()
                                .map(PoseBadLogEntity::getTimestamp)
                                .distinct()
                                .sorted()
                                .toList()
                )
        );

        return stats;
    }

    private int calculateBadCount(List<Integer> timestamps) {

        if (timestamps.isEmpty()) {
            return 0;
        }

        int count = 1;

        for (int i = 1; i < timestamps.size(); i++) {

            if (timestamps.get(i) - timestamps.get(i - 1) > 1) {
                count++;
            }
        }

        return count;
    }

    private PoseScoreResult calculateScoreResult(int badDuration, int totalDuration) {
        double badRatio = (double) badDuration / totalDuration;

        int score = Math.max(0, 100 - (int) (badRatio * 100));

        PoseGrade grade = calculateGrade(score);

        return PoseScoreResult.builder()
                .badRatio(badRatio)
                .score(score)
                .grade(grade)
                .build();
    }

    private PoseGrade calculateGrade(int score) {
        if (score >= 90) return PoseGrade.A;
        else if (score >= 80) return PoseGrade.B;
        else if (score >= 70) return PoseGrade.C;
        else if (score >= 60) return PoseGrade.D;
        else return PoseGrade.E;
    }

    private PoseAnalysisResultEntity createEmptyResult(Integer totalDuration) {
        return PoseAnalysisResultEntity.builder()
                .totalDuration(totalDuration)
                .badCount(0)
                .badDuration(0)
                .shoulderBadCount(0)
                .faceBadCount(0)
                .shoulderBadDuration(0)
                .faceBadDuration(0)
                .badRatio(0.0)
                .score(100)
                .grade(PoseGrade.A)
                .build();
    }
}
