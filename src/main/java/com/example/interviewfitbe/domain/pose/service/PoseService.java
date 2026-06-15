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
import com.example.interviewfitbe.domain.pose.exception.PoseAnalysisResultErrorCode;
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
        return this.poseAnalysisResultRepository.findAll().stream()
                .map(this.poseMapper::toDto)
                .toList();
    }

    public PoseAnalysisResultDto getPoseAnalysis(Long id) {
        return this.poseMapper.toDto(
                this.poseAnalysisResultRepository.findById(id).orElseThrow(() ->
                        new BaseException(PoseAnalysisResultErrorCode.POSE_ANALYSIS_RESULT_NOT_FOUND))
        );
    }

    /**
     * 기존 interview 단위 자세 분석.
     */
    public PoseAnalysisResultDto analyzeFullPosture(
            String videoObjectKey,
            InterviewEntity interviewEntity
    ) {
        return analyzeFullPostureInternal(videoObjectKey, interviewEntity, null);
    }

    /**
     * 질문별 답변 영상 자세 분석.
     *
     * interview_id와 interview_answer_id를 함께 저장한다.
     */
    public PoseAnalysisResultDto analyzeFullPosture(
            String videoObjectKey,
            InterviewAnswerEntity interviewAnswer
    ) {
        return analyzeFullPostureInternal(
                videoObjectKey,
                interviewAnswer.getInterview(),
                interviewAnswer
        );
    }

    private PoseAnalysisResultDto analyzeFullPostureInternal(
            String videoObjectKey,
            InterviewEntity interviewEntity,
            InterviewAnswerEntity interviewAnswer
    ) {
        List<PoseAnalysisResponse> responses = this.poseAnalysisService.analyzePosture(videoObjectKey);

        Integer totalDuration = responses.size();

        List<PoseBadLogEntity> logs = createBadLog(responses);

        if (logs.isEmpty()) {
            return createEmptyBadLogResult(
                    logs,
                    totalDuration,
                    interviewEntity,
                    interviewAnswer
            );
        }

        return analyzePosture(
                logs,
                totalDuration,
                interviewEntity,
                interviewAnswer
        );
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

    private PoseAnalysisResultDto createEmptyBadLogResult(
            List<PoseBadLogEntity> logs,
            Integer totalDuration,
            InterviewEntity interviewEntity,
            InterviewAnswerEntity interviewAnswer
    ) {
        PoseAnalysisResultEntity saved = this.poseAnalysisResultRepository.save(
                createEmptyResult(totalDuration, interviewEntity, interviewAnswer)
        );

        for (PoseBadLogEntity entity : logs) {
            entity.assignPoseAnalysisResult(saved);
        }

        this.poseBadLogRepository.saveAll(logs);

        return this.poseMapper.toDto(saved);
    }

    private PoseAnalysisResultDto analyzePosture(
            List<PoseBadLogEntity> logs,
            Integer totalDuration,
            InterviewEntity interviewEntity,
            InterviewAnswerEntity interviewAnswer
    ) {
        PoseStatistics stats = calculateStatistics(logs);

        PoseScoreResult result = calculateScoreResult(stats.getBadDuration(), totalDuration);

        PoseAnalysisResultEntity saved = this.poseAnalysisResultRepository.save(
                PoseAnalysisResultEntity.builder()
                        .interview(interviewEntity)
                        .interviewAnswer(interviewAnswer)
                        .totalDuration(totalDuration)
                        .badCount(stats.getBadCount())
                        .badDuration(stats.getBadDuration())
                        .shoulderBadCount(stats.getShoulderBadCount())
                        .faceBadCount(stats.getFaceBadCount())
                        .shoulderBadDuration(stats.getShoulderBadDuration())
                        .faceBadDuration(stats.getFaceBadDuration())
                        .badRatio(result.getBadRatio())
                        .score(result.getScore())
                        .grade(result.getGrade())
                        .build()
        );

        for (PoseBadLogEntity entity : logs) {
            entity.assignPoseAnalysisResult(saved);
        }

        this.poseBadLogRepository.saveAll(logs);

        return this.poseMapper.toDto(saved);
    }

    private PoseStatistics calculateStatistics(List<PoseBadLogEntity> logs) {
        PoseStatistics stats = new PoseStatistics();

        stats.setBadDuration(
                (int) logs.stream()
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .count()
        );

        stats.setShoulderBadDuration(
                (int) logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.SHOULDER)
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .count()
        );

        stats.setFaceBadDuration(
                (int) logs.stream()
                        .filter(log -> log.getIssueType() == PoseIssueType.FACE)
                        .map(PoseBadLogEntity::getTimestamp)
                        .distinct()
                        .count()
        );

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
        if (totalDuration <= 0) {
            return PoseScoreResult.builder()
                    .badRatio(0.0)
                    .score(100)
                    .grade(PoseGrade.A)
                    .build();
        }

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

    private PoseAnalysisResultEntity createEmptyResult(
            Integer totalDuration,
            InterviewEntity interview,
            InterviewAnswerEntity interviewAnswer
    ) {
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