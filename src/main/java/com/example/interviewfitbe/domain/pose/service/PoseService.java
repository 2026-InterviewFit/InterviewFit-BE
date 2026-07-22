package com.example.interviewfitbe.domain.pose.service;

import com.example.interviewfitbe.domain.interview.entity.InterviewEntity;
import com.example.interviewfitbe.domain.pose.calculator.PoseScoreCalculator;
import com.example.interviewfitbe.domain.pose.calculator.PoseStatisticsCalculator;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResponse;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseLogs;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseNotDetectedStatistics;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseScoreResult;
import com.example.interviewfitbe.domain.pose.dto.statistics.PoseStatistics;
import com.example.interviewfitbe.domain.pose.entity.PoseReportEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseBadLogEntity;
import com.example.interviewfitbe.domain.pose.entity.PoseNotDetectedLogEntity;
import com.example.interviewfitbe.domain.pose.enums.PoseIssueType;
import com.example.interviewfitbe.domain.pose.enums.PoseStatus;
import com.example.interviewfitbe.domain.pose.exception.PoseErrorCode;
import com.example.interviewfitbe.domain.pose.mapper.PoseMapper;
import com.example.interviewfitbe.domain.pose.repository.PoseReportRepository;
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
    private final PoseReportRepository poseReportRepository;
    private final PoseMapper poseMapper;
    private final PoseStatisticsCalculator poseStatisticsCalculator;
    private final PoseScoreCalculator poseScoreCalculator;

    public List<PoseAnalysisResultDto> getAllPostureAnalysis() {
        return poseReportRepository.findAll().stream()
                .map(poseMapper::toDto)
                .toList();
    }

    public PoseAnalysisResultDto getPoseAnalysis(Long id) {
        return poseMapper.toDto(
                poseReportRepository.findById(id).orElseThrow(() ->
                        new BaseException(PoseErrorCode.POSE_ANALYSIS_RESULT_NOT_FOUND))
        );
    }

    public void processPostureAnalysis(String videoObjectKey, InterviewEntity interview) {
        // 1. FastAPI 분석 결과 (전체 기준)
        List<PoseAnalysisResponse> responses = poseAnalysisService.analyzePosture(videoObjectKey);
        double totalDuration = calculateTotalDuration(responses);

        if (totalDuration <= 0) {
            saveEmptyAnalysisResult(interview);
            return;
        }

        // 2. BAD 로그 및 Not Detected 로그 생성
        PoseLogs logs = createPoseLogs(responses);

        // 3. 분석
        createAndSaveAnalysisResult(logs.getBadLogs(), logs.getNotDetectedLogs(), totalDuration, interview);
    }

    private double calculateTotalDuration(List<PoseAnalysisResponse> responses) {
        return responses.isEmpty()
                ? 0.0
                : responses.get(responses.size() - 1).getTimestamp();
    }

    private void  saveEmptyAnalysisResult(InterviewEntity interview) {
        poseReportRepository.save(
                poseMapper.toEmptyResult(interview)
        );
    }

    private PoseLogs createPoseLogs(List<PoseAnalysisResponse> responses) {
        List<PoseBadLogEntity> badLogs = new ArrayList<>();
        List<PoseNotDetectedLogEntity> notDetectedLogs = new ArrayList<>();

        for (PoseAnalysisResponse analysis : responses) {
            if (PoseStatus.BAD.name().equals(analysis.getShoulderStatus())) {
                badLogs.add(
                        poseMapper.toBadLog(analysis.getTimestamp(), PoseIssueType.SHOULDER, analysis.getShoulderTilt())
                );
            } else if (PoseStatus.NOT_DETECTED.name().equals(analysis.getShoulderStatus())) {
                notDetectedLogs.add(
                        poseMapper.toNotDetectedLog(analysis.getTimestamp(), PoseIssueType.SHOULDER)
                );
            }
            if (PoseStatus.BAD.name().equals(analysis.getFaceStatus())) {
                badLogs.add(
                        poseMapper.toBadLog(analysis.getTimestamp(), PoseIssueType.FACE, analysis.getFaceDirection())
                );
            } else if (PoseStatus.NOT_DETECTED.name().equals(analysis.getFaceStatus())) {
                notDetectedLogs.add(
                        poseMapper.toNotDetectedLog(analysis.getTimestamp(), PoseIssueType.FACE)
                );
            }
        }

        return poseMapper.toPoseLogs(badLogs, notDetectedLogs);
    }

    private void createAndSaveAnalysisResult(List<PoseBadLogEntity> badLogs, List<PoseNotDetectedLogEntity> notDetectedLogs, Double totalDuration, InterviewEntity interview) {
        PoseStatistics stats = poseStatisticsCalculator.calculateStatistics(badLogs);
        PoseNotDetectedStatistics notDetectedStats = poseStatisticsCalculator.calculateNotDetectedStatistics(notDetectedLogs);
        PoseScoreResult result = poseScoreCalculator.calculateScoreResult(stats.getBadDuration(), notDetectedStats.getNotDetectedDuration(), totalDuration);

        PoseReportEntity saved = poseReportRepository.save(
                poseMapper.toEntity(totalDuration, stats, notDetectedStats, result, interview)
        );

        saveBadLogs(badLogs, saved);
        saveNotDetectedLogs(notDetectedLogs, saved);
    }

    private void saveBadLogs(List<PoseBadLogEntity> badLogs, PoseReportEntity poseReport) {
        if (badLogs.isEmpty()) {
            return;
        }

        badLogs.forEach(log -> log.assignPoseReport(poseReport));
        poseBadLogRepository.saveAll(badLogs);
    }

    private void saveNotDetectedLogs(List<PoseNotDetectedLogEntity> notDetectedLogs, PoseReportEntity poseReport) {
        if (notDetectedLogs.isEmpty()) {
            return;
        }

        notDetectedLogs.forEach(log -> log.assignPoseReport(poseReport));
        poseNotDetectedLogRepository.saveAll(notDetectedLogs);
    }
}