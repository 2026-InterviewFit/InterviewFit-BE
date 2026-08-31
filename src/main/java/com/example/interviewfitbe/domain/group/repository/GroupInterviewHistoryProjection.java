package com.example.interviewfitbe.domain.group.repository;

import java.time.LocalDateTime;

public interface GroupInterviewHistoryProjection {
    Long getRoomId();
    String getTitle();
    Long getJobRoleId();
    String getJobCategory();
    String getJobName();
    LocalDateTime getStartedAt();
    LocalDateTime getEndedAt();
    String getRoomStatus();
    Long getParticipantCount();
    Long getTotalAnswers();
    Long getUploadedAnalyses();
    Long getRunningAnalyses();
    Long getCompletedAnalyses();
    Long getFailedAnalyses();
}
