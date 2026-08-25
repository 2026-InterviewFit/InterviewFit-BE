package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.*;
import com.example.interviewfitbe.domain.group.repository.*;
import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class GroupInterviewComparisonServiceTest {
    private GroupInterviewAnswerAnalysisRepository analysisRepository;
    private GroupInterviewComparisonReportRepository reportRepository;
    private GroupInterviewComparisonScoreCalculator calculator;
    private GroupInterviewComparisonService service;
    private final List<GroupInterviewComparisonReportEntity> saved = new ArrayList<>();

    @BeforeEach
    void setUp() {
        GroupInterviewRepository interviewRepository = mock(GroupInterviewRepository.class);
        GroupInterviewSessionRepository sessionRepository = mock(GroupInterviewSessionRepository.class);
        GroupParticipantRepository participantRepository = mock(GroupParticipantRepository.class);
        analysisRepository = mock(GroupInterviewAnswerAnalysisRepository.class);
        reportRepository = mock(GroupInterviewComparisonReportRepository.class);
        calculator = mock(GroupInterviewComparisonScoreCalculator.class);
        GroupComparisonFeedbackClient feedbackClient = mock(GroupComparisonFeedbackClient.class);

        GroupInterviewEntity interview = mock(GroupInterviewEntity.class);
        when(interview.getRoomStatus()).thenReturn("COMPLETED");
        GroupInterviewSessionEntity session = mock(GroupInterviewSessionEntity.class);
        when(session.getStatus()).thenReturn(InterviewStatus.FINISHED);
        when(interviewRepository.findById(10L)).thenReturn(Optional.of(interview));
        when(sessionRepository.findByInterviewIdForUpdate(10L)).thenReturn(Optional.of(session));
        when(participantRepository.findAllByRoomIdOrderByIdAsc(10L)).thenReturn(List.of(
                GroupParticipantEntity.builder().roomId(10L).userId(1L).build(),
                GroupParticipantEntity.builder().roomId(10L).userId(2L).build()));
        when(reportRepository.findByRoomIdAndParticipantUserIdAndScopeAndQuestionIndex(
                anyLong(), anyLong(), any(), anyInt())).thenReturn(Optional.empty());
        when(reportRepository.findAllByRoomIdAndScopeOrderByRankAscParticipantUserIdAsc(
                10L, GroupComparisonScope.QUESTION)).thenAnswer(invocation -> saved.stream()
                .filter(r -> r.getScope() == GroupComparisonScope.QUESTION).toList());
        when(reportRepository.save(any())).thenAnswer(invocation -> {
            GroupInterviewComparisonReportEntity report = invocation.getArgument(0);
            if (!saved.contains(report)) saved.add(report);
            return report;
        });
        when(feedbackClient.createFeedback(any())).thenReturn(List.of());

        service = new GroupInterviewComparisonService(interviewRepository, sessionRepository,
                participantRepository, analysisRepository, reportRepository, calculator, feedbackClient);
    }

    @Test
    void createsQuestionRanksOnlyWhenEveryAnalysisIsDone() {
        var first = analysis(1L, AnalysisStatus.DONE);
        var second = analysis(2L, AnalysisStatus.DONE);
        when(analysisRepository.findAllForComparison(10L, 0)).thenReturn(List.of(first, second));
        when(analysisRepository.findAllForComparison(10L, 1)).thenReturn(List.of());
        when(analysisRepository.findAllForComparison(10L, 2)).thenReturn(List.of());
        when(calculator.calculate(first)).thenReturn(scores("90"));
        when(calculator.calculate(second)).thenReturn(scores("80"));

        service.generateAvailableComparisons(10L);

        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(r -> r.getStatus() == GroupComparisonStatus.DONE);
        assertThat(saved).filteredOn(r -> r.getParticipantUserId().equals(1L))
                .singleElement().extracting(GroupInterviewComparisonReportEntity::getRank).isEqualTo(1);
        assertThat(saved).filteredOn(r -> r.getParticipantUserId().equals(2L))
                .singleElement().extracting(GroupInterviewComparisonReportEntity::getRank).isEqualTo(2);
    }

    @Test
    void failedAnalysisCreatesPartialWithoutRanks() {
        var done = analysis(1L, AnalysisStatus.DONE);
        var failed = analysis(2L, AnalysisStatus.FAILED);
        when(analysisRepository.findAllForComparison(10L, 0)).thenReturn(List.of(done, failed));
        when(analysisRepository.findAllForComparison(10L, 1)).thenReturn(List.of());
        when(analysisRepository.findAllForComparison(10L, 2)).thenReturn(List.of());
        when(calculator.calculate(done)).thenReturn(scores("90"));

        service.generateAvailableComparisons(10L);

        assertThat(saved).hasSize(2);
        assertThat(saved).allMatch(r -> r.getStatus() == GroupComparisonStatus.PARTIAL);
        assertThat(saved).allMatch(r -> r.getRank() == null);
        assertThat(saved).filteredOn(r -> r.getParticipantUserId().equals(2L))
                .singleElement().extracting(GroupInterviewComparisonReportEntity::getTotalScore).isNull();
    }

    private GroupInterviewAnswerAnalysisEntity analysis(Long participantId, AnalysisStatus status) {
        GroupInterviewEntity room = mock(GroupInterviewEntity.class);
        when(room.getId()).thenReturn(10L);
        GroupInterviewAnswerEntity answer = GroupInterviewAnswerEntity.builder()
                .groupInterview(room).participantUserId(participantId).questionIndex(0).build();
        return GroupInterviewAnswerAnalysisEntity.builder().answer(answer).status(status).build();
    }

    private GroupInterviewComparisonScoreCalculator.Scores scores(String value) {
        BigDecimal score = new BigDecimal(value);
        return new GroupInterviewComparisonScoreCalculator.Scores(
                score, score, score, score, score, score);
    }
}
