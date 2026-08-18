package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.entity.GroupAnalysisStatus;
import com.example.interviewfitbe.domain.group.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class GroupInterviewDashboardServiceTest {

    private GroupInterviewRepository interviewRepository;
    private GroupInterviewAnswerRepository answerRepository;
    private GroupParticipantRepository participantRepository;
    private GroupInterviewDashboardService service;

    @BeforeEach
    void setUp() {
        interviewRepository = mock(GroupInterviewRepository.class);
        answerRepository = mock(GroupInterviewAnswerRepository.class);
        participantRepository = mock(GroupParticipantRepository.class);
        service = new GroupInterviewDashboardService(
                interviewRepository,
                answerRepository,
                participantRepository
        );
    }

    @Test
    void historyAggregatesDoneWhenEveryAnswerAnalysisIsDone() {
        GroupInterviewHistoryProjection projection = history(3L, 0L, 0L, 3L, 0L);
        when(interviewRepository.findCompletedHistoryByUserId(5L))
                .thenReturn(List.of(projection));

        var result = service.getMyHistory(5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnalysisStatus()).isEqualTo(GroupAnalysisStatus.DONE);
        assertThat(result.get(0).getCompletedAnalyses()).isEqualTo(3L);
    }

    @Test
    void historyPrioritizesFailedStatus() {
        GroupInterviewHistoryProjection projection = history(3L, 0L, 0L, 2L, 1L);
        when(interviewRepository.findCompletedHistoryByUserId(5L))
                .thenReturn(List.of(projection));

        assertThat(service.getMyHistory(5L).get(0).getAnalysisStatus())
                .isEqualTo(GroupAnalysisStatus.FAILED);
    }

    @Test
    void myAnswersIncludeQuestionAndAnalysisWithoutClientUserId() {
        GroupInterviewMyAnswerProjection projection = mock(GroupInterviewMyAnswerProjection.class);
        when(projection.getAnswerId()).thenReturn(123L);
        when(projection.getRoundNumber()).thenReturn(1);
        when(projection.getQuestionIndex()).thenReturn(1);
        when(projection.getQuestionId()).thenReturn(30L);
        when(projection.getQuestionText()).thenReturn("지원 동기를 말씀해 주세요.");
        when(projection.getCompletionType()).thenReturn("COMPLETED");
        when(projection.getVideoUploaded()).thenReturn(true);
        when(projection.getAnalysisStatus()).thenReturn("RUNNING");
        when(interviewRepository.existsById(10L)).thenReturn(true);
        when(participantRepository.existsByRoomIdAndUserId(10L, 5L)).thenReturn(true);
        when(answerRepository.findMyAnswersWithAnalysis(10L, 5L))
                .thenReturn(List.of(projection));

        var result = service.getMyAnswers(10L, 5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getAnswerId()).isEqualTo(123L);
        assertThat(result.get(0).getQuestionText()).contains("지원 동기");
        assertThat(result.get(0).getAnalysisStatus()).isEqualTo(GroupAnalysisStatus.RUNNING);
    }

    private GroupInterviewHistoryProjection history(
            Long total,
            Long uploaded,
            Long running,
            Long done,
            Long failed
    ) {
        GroupInterviewHistoryProjection projection = mock(GroupInterviewHistoryProjection.class);
        when(projection.getRoomId()).thenReturn(10L);
        when(projection.getTitle()).thenReturn("백엔드 그룹 면접");
        when(projection.getParticipantCount()).thenReturn(3L);
        when(projection.getTotalAnswers()).thenReturn(total);
        when(projection.getUploadedAnalyses()).thenReturn(uploaded);
        when(projection.getRunningAnalyses()).thenReturn(running);
        when(projection.getCompletedAnalyses()).thenReturn(done);
        when(projection.getFailedAnalyses()).thenReturn(failed);
        return projection;
    }
}
