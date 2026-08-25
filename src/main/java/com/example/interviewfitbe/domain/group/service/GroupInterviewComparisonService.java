package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupComparisonFeedbackRequest;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewComparisonResponse;
import com.example.interviewfitbe.domain.group.entity.*;
import com.example.interviewfitbe.domain.group.repository.*;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class GroupInterviewComparisonService {
    private static final int QUESTION_COUNT = 3;

    private final GroupInterviewRepository interviewRepository;
    private final GroupInterviewSessionRepository sessionRepository;
    private final GroupParticipantRepository participantRepository;
    private final GroupInterviewAnswerAnalysisRepository analysisRepository;
    private final GroupInterviewComparisonReportRepository reportRepository;
    private final GroupInterviewComparisonScoreCalculator scoreCalculator;
    private final GroupComparisonFeedbackClient feedbackClient;
    private final UserRepository userRepository;

    @Transactional
    public void generateAvailableComparisons(Long roomId) {
        var interview = interviewRepository.findById(roomId).orElse(null);
        if (interview == null || !"COMPLETED".equals(interview.getRoomStatus())) return;
        // 분석 완료 콜백이 동시에 들어와도 방별로 한 번씩 직렬화한다.
        var session = sessionRepository.findByInterviewIdForUpdate(roomId).orElse(null);
        if (session == null || session.getStatus() != InterviewStatus.FINISHED) return;

        List<Long> participantIds = participantRepository.findAllByRoomIdOrderByIdAsc(roomId)
                .stream().map(GroupParticipantEntity::getUserId).toList();
        if (participantIds.size() < 2) return;
        for (int questionIndex = 0; questionIndex < QUESTION_COUNT; questionIndex++) {
            generateQuestionIfTerminal(roomId, questionIndex, participantIds);
        }
        generateOverallIfReady(roomId, participantIds);
    }

    @Transactional(readOnly = true)
    public List<GroupInterviewComparisonResponse> getOverall(Long roomId, Long userId) {
        validateParticipant(roomId, userId);
        return toResponses(reportRepository.findAllByRoomIdAndScopeOrderByRankAscParticipantUserIdAsc(
                roomId, GroupComparisonScope.OVERALL));
    }

    @Transactional(readOnly = true)
    public List<GroupInterviewComparisonResponse> getQuestion(Long roomId, int questionIndex, Long userId) {
        validateQuestionIndex(questionIndex);
        validateParticipant(roomId, userId);
        return toResponses(reportRepository
                .findAllByRoomIdAndScopeAndQuestionIndexOrderByRankAscParticipantUserIdAsc(
                        roomId, GroupComparisonScope.QUESTION, questionIndex));
    }

    private void generateQuestionIfTerminal(Long roomId, int questionIndex, List<Long> participantIds) {
        List<GroupInterviewAnswerAnalysisEntity> analyses =
                analysisRepository.findAllForComparison(roomId, questionIndex);
        Map<Long, GroupInterviewAnswerAnalysisEntity> byParticipant = analyses.stream()
                .collect(Collectors.toMap(a -> a.getAnswer().getParticipantUserId(), Function.identity(),
                        (first, ignored) -> first));
        if (!byParticipant.keySet().containsAll(participantIds)) return;
        if (participantIds.stream().map(byParticipant::get)
                .anyMatch(a -> a.getStatus() != AnalysisStatus.DONE && a.getStatus() != AnalysisStatus.FAILED)) return;

        boolean hasFailed = participantIds.stream().map(byParticipant::get)
                .anyMatch(a -> a.getStatus() == AnalysisStatus.FAILED);
        Map<Long, GroupInterviewComparisonScoreCalculator.Scores> scores = new LinkedHashMap<>();
        for (Long participantId : participantIds) {
            var analysis = byParticipant.get(participantId);
            scores.put(participantId, analysis.getStatus() == AnalysisStatus.DONE
                    ? scoreCalculator.calculate(analysis) : emptyScores());
        }
        boolean complete = !hasFailed && scores.values().stream().allMatch(
                GroupInterviewComparisonScoreCalculator.Scores::complete);
        Map<Long, Integer> ranks = complete ? rank(scores) : Collections.emptyMap();
        GroupComparisonStatus status = complete ? GroupComparisonStatus.DONE : GroupComparisonStatus.PARTIAL;

        for (Long participantId : participantIds) {
            var value = scores.get(participantId);
            var report = findOrCreate(roomId, participantId, GroupComparisonScope.QUESTION, questionIndex,
                    participantIds.size(), status);
            report.updateScores(value.content(), value.speech(), value.gaze(), value.pose(), value.blink(),
                    value.total(), ranks.get(participantId), participantIds.size(), status);
            reportRepository.save(report);
        }
    }

    private void generateOverallIfReady(Long roomId, List<Long> participantIds) {
        List<GroupInterviewComparisonReportEntity> questions = reportRepository
                .findAllByRoomIdAndScopeOrderByRankAscParticipantUserIdAsc(roomId, GroupComparisonScope.QUESTION);
        Map<Long, List<GroupInterviewComparisonReportEntity>> byParticipant = questions.stream()
                .filter(r -> r.getStatus() == GroupComparisonStatus.DONE)
                .collect(Collectors.groupingBy(GroupInterviewComparisonReportEntity::getParticipantUserId));
        if (participantIds.stream().anyMatch(id -> byParticipant.getOrDefault(id, List.of()).size() != QUESTION_COUNT)) {
            return;
        }
        List<GroupInterviewComparisonReportEntity> existingOverall = reportRepository
                .findAllByRoomIdAndScopeOrderByRankAscParticipantUserIdAsc(
                        roomId, GroupComparisonScope.OVERALL);
        if (existingOverall.size() == participantIds.size()
                && existingOverall.stream().map(GroupInterviewComparisonReportEntity::getParticipantUserId)
                .collect(Collectors.toSet()).containsAll(participantIds)) {
            return;
        }
        Map<Long, GroupInterviewComparisonScoreCalculator.Scores> averages = new LinkedHashMap<>();
        for (Long participantId : participantIds) averages.put(participantId, average(byParticipant.get(participantId)));
        Map<Long, Integer> ranks = rank(averages);
        List<GroupInterviewComparisonReportEntity> saved = new ArrayList<>();
        for (Long participantId : participantIds) {
            var value = averages.get(participantId);
            var report = findOrCreate(roomId, participantId, GroupComparisonScope.OVERALL, null,
                    participantIds.size(), GroupComparisonStatus.DONE);
            report.updateScores(value.content(), value.speech(), value.gaze(), value.pose(), value.blink(),
                    value.total(), ranks.get(participantId), participantIds.size(), GroupComparisonStatus.DONE);
            saved.add(reportRepository.save(report));
        }
        applyFeedback(roomId, null, saved);
    }

    private GroupInterviewComparisonReportEntity findOrCreate(
            Long roomId, Long participantId, GroupComparisonScope scope, Integer questionIndex,
            int participantCount, GroupComparisonStatus status) {
        Optional<GroupInterviewComparisonReportEntity> existing = questionIndex == null
                ? reportRepository.findByRoomIdAndParticipantUserIdAndScopeAndQuestionIndexIsNull(
                roomId, participantId, scope)
                : reportRepository.findByRoomIdAndParticipantUserIdAndScopeAndQuestionIndex(
                roomId, participantId, scope, questionIndex);
        return existing.orElseGet(() -> GroupInterviewComparisonReportEntity.builder()
                .roomId(roomId).participantUserId(participantId).scope(scope)
                .questionIndex(questionIndex).participantCount(participantCount).status(status).build());
    }

    private Map<Long, Integer> rank(Map<Long, GroupInterviewComparisonScoreCalculator.Scores> scores) {
        List<Map.Entry<Long, GroupInterviewComparisonScoreCalculator.Scores>> sorted = scores.entrySet().stream()
                .sorted(Map.Entry.<Long, GroupInterviewComparisonScoreCalculator.Scores>comparingByValue(
                        Comparator.comparing(GroupInterviewComparisonScoreCalculator.Scores::total)).reversed()
                        .thenComparing(Map.Entry::getKey)).toList();
        Map<Long, Integer> result = new HashMap<>();
        BigDecimal previous = null;
        int previousRank = 0;
        for (int i = 0; i < sorted.size(); i++) {
            BigDecimal total = sorted.get(i).getValue().total();
            int currentRank = previous != null && previous.compareTo(total) == 0 ? previousRank : i + 1;
            result.put(sorted.get(i).getKey(), currentRank);
            previous = total;
            previousRank = currentRank;
        }
        return result;
    }

    private GroupInterviewComparisonScoreCalculator.Scores average(
            List<GroupInterviewComparisonReportEntity> reports) {
        return new GroupInterviewComparisonScoreCalculator.Scores(
                average(reports, GroupInterviewComparisonReportEntity::getContentScore),
                average(reports, GroupInterviewComparisonReportEntity::getSpeechScore),
                average(reports, GroupInterviewComparisonReportEntity::getGazeScore),
                average(reports, GroupInterviewComparisonReportEntity::getPoseScore),
                average(reports, GroupInterviewComparisonReportEntity::getBlinkScore),
                average(reports, GroupInterviewComparisonReportEntity::getTotalScore));
    }

    private BigDecimal average(List<GroupInterviewComparisonReportEntity> reports,
                               Function<GroupInterviewComparisonReportEntity, BigDecimal> getter) {
        return reports.stream().map(getter).reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(reports.size()), 2, RoundingMode.HALF_UP);
    }

    private void applyFeedback(Long roomId, Integer questionIndex,
                               List<GroupInterviewComparisonReportEntity> reports) {
        var participants = reports.stream().map(r -> new GroupComparisonFeedbackRequest.Participant(
                r.getParticipantUserId(), r.getContentScore(), r.getSpeechScore(), r.getGazeScore(),
                r.getPoseScore(), r.getBlinkScore(), r.getTotalScore(), r.getRank())).toList();
        Map<Long, GroupComparisonFeedbackClient.Feedback> feedback = feedbackClient
                .createFeedback(new GroupComparisonFeedbackRequest(roomId, questionIndex, participants))
                .stream().collect(Collectors.toMap(GroupComparisonFeedbackClient.Feedback::participantUserId,
                        Function.identity(), (first, ignored) -> first));
        reports.forEach(report -> {
            var value = feedback.get(report.getParticipantUserId());
            if (value != null) {
                report.updateFeedback(value.strengthSummary(), value.improvementSummary());
                reportRepository.save(report);
            }
        });
    }

    private List<GroupInterviewComparisonResponse> toResponses(
            List<GroupInterviewComparisonReportEntity> reports
    ) {
        Map<Long, String> participantNames = userRepository.findAllById(
                        reports.stream().map(GroupInterviewComparisonReportEntity::getParticipantUserId).toList())
                .stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getName));
        return reports.stream()
                .map(report -> GroupInterviewComparisonResponse.from(
                        report, participantNames.get(report.getParticipantUserId())))
                .toList();
    }

    private void validateParticipant(Long roomId, Long userId) {
        if (!interviewRepository.existsById(roomId)) throw new IllegalArgumentException("면접 방을 찾을 수 없습니다.");
        if (!participantRepository.existsByRoomIdAndUserId(roomId, userId))
            throw new AccessDeniedException("해당 면접 방의 참가자만 상대 비교를 조회할 수 있습니다.");
    }
    private void validateQuestionIndex(int questionIndex) {
        if (questionIndex < 0 || questionIndex >= QUESTION_COUNT)
            throw new IllegalArgumentException("질문 인덱스는 0부터 2까지입니다.");
    }
    private GroupInterviewComparisonScoreCalculator.Scores emptyScores() {
        return new GroupInterviewComparisonScoreCalculator.Scores(null, null, null, null, null, null);
    }
}
