package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.GroupAnalysisStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class GroupInterviewHistoryResponse {
    private Long roomId;
    private String title;
    private Long jobRoleId;
    private String jobCategory;
    private String jobName;
    private LocalDateTime startedAt;
    private LocalDateTime endedAt;
    private String roomStatus;
    private long participantCount;
    private long totalAnswers;
    private long completedAnalyses;
    private long failedAnalyses;
    private GroupAnalysisStatus analysisStatus;
}
