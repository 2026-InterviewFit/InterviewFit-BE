package com.example.interviewfitbe.domain.pose.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewAnswerEntity;
import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResponse;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseScoreResult;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseStatistics;
import com.example.interviewfitbe.domain.pose.entity.PoseAnalysisResultEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseBadLogEntity;
import com.example.interviewfitbe.domain.pose.enums.PoseGrade;
import com.example.interviewfitbe.domain.pose.enums.PoseIssueType;
import com.example.interviewfitbe.domain.pose.exception.PoseErrorCode;
import com.example.interviewfitbe.domain.pose.mapper.PoseMapper;
import com.example.interviewfitbe.domain.pose.repository.PoseAnalysisResultRepository;
import com.example.interviewfitbe.domain.pose.repository.PoseBadLogRepository;
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

    // 나중에 삭제하기
    public void analyzeFullPosture(String videoObjectKey, InterviewEntity interviewEntity) {
        List<PoseAnalysisResponse> responses = poseAnalysisService.analyzePosture(videoObjectKey);
        Integer totalDuration = responses.size();
        List<PoseBadLogEntity> logs = createBadLog(responses);
        if (logs.isEmpty()) {
            createEmptyBadLogResult(logs, totalDuration, interviewEntity, null);
            return;
        }
        analyzePosture(logs, totalDuration, interviewEntity, null);
    }

    public void analyzeFullPosture(String videoObjectKey, InterviewAnswerEntity interviewAnswer) {
        // 1. FastAPI 분석 결과 (전체 기준)
        List<PoseAnalysisResponse> responses = poseAnalysisService.analyzePosture(videoObjectKey);
        Integer totalDuration = responses.size();

        // 2. BAD 로그 생성
        List<PoseBadLogEntity> logs = createBadLog(responses);

        // 3. 분석
        if (logs.isEmpty()) {
            createEmptyBadLogResult(logs, totalDuration, interviewAnswer.getInterview(), interviewAnswer);
        } else {
            analyzePosture(logs, totalDuration, interviewAnswer.getInterview(), interviewAnswer);
        }
    }

    private void  createEmptyBadLogResult(List<PoseBadLogEntity> logs, Integer totalDuration, InterviewEntity interviewEntity, InterviewAnswerEntity interviewAnswer) {
        PoseAnalysisResultEntity saved = poseAnalysisResultRepository.save(
                createEmptyResult(totalDuration, interviewEntity, interviewAnswer)
        );

        logs.forEach(entity -> entity.assignPoseAnalysisResult(saved));
        poseBadLogRepository.saveAll(logs);
    }

    private List<PoseBadLogEntity> createBadLog(List<PoseAnalysisResponse> responses) {
        List<PoseBadLogEntity> logs = new ArrayList<>();

        for (PoseAnalysisResponse analysis : responses) {
            if ("ShoulderStatus.BAD".equals(analysis.getShoulderStatus())) {
                logs.add(PoseBadLogEntity.builder()
                        .timestamp(analysis.getTimestamp())
                        .issueType(PoseIssueType.SHOULDER)
                        .deviation(analysis.getShoulderTilt())
                        .build());
            }
            if ("FaceStatus.BAD".equals(analysis.getFaceStatus())) {
                logs.add(PoseBadLogEntity.builder()
                        .timestamp(analysis.getTimestamp())
                        .issueType(PoseIssueType.FACE)
                        .deviation(analysis.getFaceDirection())
                        .build());
            }
        }

        return logs;
    }

    private void analyzePosture(List<PoseBadLogEntity> logs, Integer totalDuration, InterviewEntity interview, InterviewAnswerEntity interviewAnswer) {
        PoseStatistics stats = calculateStatistics(logs);
        PoseScoreResult result = calculateScoreResult(stats.getBadDuration(), totalDuration);

        PoseAnalysisResultEntity saved = poseAnalysisResultRepository.save(
                poseMapper.toEntity(totalDuration, stats, result, interview, interviewAnswer)
        );

        logs.forEach(entity -> entity.assignPoseAnalysisResult(saved));
        poseBadLogRepository.saveAll(logs);
    }

    private PoseStatistics calculateStatistics(List<PoseBadLogEntity> logs) {
        return PoseStatistics.builder()
                .badDuration((int) logs.stream()
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .count())
                .shoulderBadDuration((int) logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.SHOULDER)
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .count())
                .faceBadDuration((int) logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.FACE)
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .count())
                .badCount(calculateBadCount(
                        logs.stream()
                                .map(PoseBadLogEntity::getTimestamp)
                                .distinct()
                                .sorted()
                                .toList()))
                .shoulderBadCount(calculateBadCount(
                        logs.stream()
                                .filter(log -> log.getIssueType() == PoseIssueType.SHOULDER)
                                .map(PoseBadLogEntity::getTimestamp)
                                .distinct()
                                .sorted()
                                .toList()))
                .faceBadCount(calculateBadCount(
                        logs.stream()
                                .filter(log -> log.getIssueType() == PoseIssueType.FACE)
                                .map(PoseBadLogEntity::getTimestamp)
                                .distinct()
                                .sorted()
                                .toList()))
                .build();
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
        if (score >= 85) return PoseGrade.A;
        else if (score >= 75) return PoseGrade.B;
        else if (score >= 65) return PoseGrade.C;
        else if (score >= 50) return PoseGrade.D;
        else return PoseGrade.E;
    }

    private PoseAnalysisResultEntity createEmptyResult(Integer totalDuration, InterviewEntity interview, InterviewAnswerEntity interviewAnswer) {
        return PoseAnalysisResultEntity.builder()
                .interview(interview)
                .interviewAnswer(interviewAnswer)
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