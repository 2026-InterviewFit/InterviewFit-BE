package com.example.interviewfitbe.domain.group.service;

import com.example.interviewfitbe.domain.group.dto.GroupInterviewHistoryResponse;
import com.example.interviewfitbe.domain.group.dto.GroupInterviewMyAnswerResponse;
import com.example.interviewfitbe.domain.group.entity.GroupAnalysisStatus;
import com.example.interviewfitbe.domain.group.entity.GroupAnswerCompletionType;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewAnswerRepository;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewHistoryProjection;
import com.example.interviewfitbe.domain.group.repository.GroupInterviewRepository;
import com.example.interviewfitbe.domain.group.repository.GroupParticipantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GroupInterviewDashboardService {

    private final GroupInterviewRepository interviewRepository;
    private final GroupInterviewAnswerRepository answerRepository;
    private final GroupParticipantRepository participantRepository;

    public List<GroupInterviewHistoryResponse> getMyHistory(Long userId) {
        return interviewRepository.findCompletedHistoryByUserId(userId)
                .stream()
                .map(this::toHistoryResponse)
                .toList();
    }

    public List<GroupInterviewMyAnswerResponse> getMyAnswers(Long roomId, Long userId) {
        if (!interviewRepository.existsById(roomId)) {
            throw new IllegalArgumentException("면접 방을 찾을 수 없습니다.");
        }
        if (!participantRepository.existsByRoomIdAndUserId(roomId, userId)) {
            throw new AccessDeniedException("해당 면접 방의 참가자만 답변을 조회할 수 있습니다.");
        }

        return answerRepository.findMyAnswersWithAnalysis(roomId, userId)
                .stream()
                .map(answer -> GroupInterviewMyAnswerResponse.builder()
                        .answerId(answer.getAnswerId())
                        .round(answer.getRoundNumber())
                        .questionIndex(answer.getQuestionIndex())
                        .questionId(answer.getQuestionId())
                        .questionText(answer.getQuestionText())
                        .answerStartedAt(answer.getAnswerStartedAt())
                        .answerCompletedAt(answer.getAnswerCompletedAt())
                        .completionType(toCompletionType(answer.getCompletionType()))
                        .videoUploaded(Boolean.TRUE.equals(answer.getVideoUploaded()))
                        .analysisStatus(toAnalysisStatus(answer.getAnalysisStatus()))
                        .build())
                .toList();
    }

    private GroupInterviewHistoryResponse toHistoryResponse(
            GroupInterviewHistoryProjection history
    ) {
        long totalAnswers = value(history.getTotalAnswers());
        long completed = value(history.getCompletedAnalyses());
        long failed = value(history.getFailedAnalyses());
        long running = value(history.getRunningAnalyses());
        long uploaded = value(history.getUploadedAnalyses());

        return GroupInterviewHistoryResponse.builder()
                .roomId(history.getRoomId())
                .title(history.getTitle())
                .jobRoleId(history.getJobRoleId())
                .jobCategory(history.getJobCategory())
                .jobName(history.getJobName())
                .startedAt(history.getStartedAt())
                .endedAt(history.getEndedAt())
                .roomStatus(history.getRoomStatus())
                .participantCount(value(history.getParticipantCount()))
                .totalAnswers(totalAnswers)
                .completedAnalyses(completed)
                .failedAnalyses(failed)
                .analysisStatus(aggregateStatus(
                        totalAnswers,
                        uploaded,
                        running,
                        completed,
                        failed
                ))
                .build();
    }

    private GroupAnalysisStatus aggregateStatus(
            long total,
            long uploaded,
            long running,
            long done,
            long failed
    ) {
        if (failed > 0) {
            return GroupAnalysisStatus.FAILED;
        }
        if (total > 0 && done == total) {
            return GroupAnalysisStatus.DONE;
        }
        if (running > 0) {
            return GroupAnalysisStatus.RUNNING;
        }
        if (uploaded > 0) {
            return GroupAnalysisStatus.UPLOADED;
        }
        return GroupAnalysisStatus.WAITING;
    }

    private GroupAnalysisStatus toAnalysisStatus(String status) {
        if (status == null || status.isBlank()) {
            return GroupAnalysisStatus.WAITING;
        }
        return GroupAnalysisStatus.valueOf(status);
    }

    private GroupAnswerCompletionType toCompletionType(String type) {
        if (type == null || type.isBlank()) {
            return null;
        }
        return GroupAnswerCompletionType.valueOf(type);
    }

    private long value(Long value) {
        return value == null ? 0L : value;
    }
}
