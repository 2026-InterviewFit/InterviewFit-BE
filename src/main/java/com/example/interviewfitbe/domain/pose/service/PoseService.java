package com.example.interviewfitbe.domain.pose.service;

import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResponse;
import com.example.interviewfitbe.domain.pose.dto.PoseAnalysisResultDto;
import com.example.interviewfitbe.domain.pose.dto.PoseRequest;
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
        List<PoseAnalysisResponse> responses = this.poseAnalysisService.analyzePosture(request.getVideoUrl());
        Integer totalDuration = responses.size();

        // 2. BAD 로그 생성 + 저장
        List<PoseBadLogEntity> logs = saveBadLog(responses);

        // 3. 분석
        return analyzePosture(logs, totalDuration);
    }

    public List<PoseBadLogEntity> saveBadLog(List<PoseAnalysisResponse> responses) {
        List<PoseBadLogEntity> logs = new ArrayList<>();

        for (PoseAnalysisResponse analysis : responses) {
            // 어깨 BAD 로그 저장
            if ("BAD".equals(analysis.getShoulderStatus())) {
                logs.add(PoseBadLogEntity.builder()
                        .timestamp(analysis.getTimestamp())
                        .issueType(PoseIssueType.SHOULDER)
                        .deviation(analysis.getShoulderTilt())
                        .build());
            }

            // 얼굴 BAD 로그 저장
            if ("BAD".equals(analysis.getFaceStatus())) {
                logs.add(PoseBadLogEntity.builder()
                        .timestamp(analysis.getTimestamp())
                        .issueType(PoseIssueType.FACE)
                        .deviation(analysis.getFaceDirection())
                        .build());
            }
        }

        return this.poseBadLogRepository.saveAll(logs);
    }

    public PoseAnalysisResultDto analyzePosture(List<PoseBadLogEntity> logs, Integer totalDuration) {
        // 1. BAD 로그가 없다면
        if (logs.isEmpty()) {
            PoseAnalysisResultEntity result = PoseAnalysisResultEntity.builder()
                    .totalDuration(totalDuration)
                    .badCount(0)
                    .badDuration(0)
                    .shoulderBadCount(0)
                    .faceBadCount(0)
                    .shoulderBadDuration(0)
                    .faceBadDuration(0)
                    .score(100)
                    .grade(PoseGrade.A)
                    .build();

            this.poseAnalysisResultRepository.save(result);

            return this.poseMapper.toDto(result);
        }

        // 집계 변수
        int badCount = logs.size();

        int shoulderBadCount = 0;
        int faceBadCount = 0;

        int shoulderBadDuration = 0;
        int faceBadDuration = 0;

        int totalBadDuration = 0;

        // 이전 상태 (연속 구간 체크용)
        boolean prevShoulderBad = false;
        boolean prevFaceBad = false;

        for (PoseBadLogEntity log : logs) {
            // 전체 BAD duration (프레임 기준)
            totalBadDuration += 1;

            // SHOULDER
            if (log.getIssueType() == PoseIssueType.SHOULDER) {
                shoulderBadDuration += 1;

                // 연속 구간이 끊겼을 때 count 증가
                if (!prevShoulderBad) {
                    shoulderBadCount++;
                }

                prevShoulderBad = true;
            } else {
                prevShoulderBad = false;
            }

            // FACE
            if (log.getIssueType() == PoseIssueType.FACE) {
                faceBadDuration += 1;

                if (!prevFaceBad) {
                    faceBadCount++;
                }

                prevFaceBad = true;
            } else {
                prevFaceBad = false;
            }
        }

        // 점수 계산 (BAD 비율 기반)
        int badRatio = totalBadDuration / totalDuration;
        int score = Math.max(0, 100 - (badRatio * 100));

        PoseGrade grade;

        if (score >= 90) grade = PoseGrade.A;
        else if (score >= 80) grade = PoseGrade.B;
        else if (score >= 70) grade = PoseGrade.C;
        else grade = PoseGrade.D;

        PoseAnalysisResultEntity result = PoseAnalysisResultEntity.builder()
                .totalDuration(totalDuration)
                .badCount(badCount)
                .badDuration(totalBadDuration)
                .shoulderBadCount(shoulderBadCount)
                .faceBadCount(faceBadCount)
                .shoulderBadDuration(shoulderBadDuration)
                .faceBadDuration(faceBadDuration)
                .score(score)
                .grade(grade)
                .build();

        this.poseAnalysisResultRepository.save(result);

        return this.poseMapper.toDto(result);
    }
}
