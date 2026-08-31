package com.example.interviewfitbe.domain.group.repository;

import java.time.Instant;

public interface GroupInterviewMyAnswerProjection {
    Long getAnswerId();
    Integer getRoundNumber();
    Integer getQuestionIndex();
    Long getQuestionId();
    String getQuestionText();
    Instant getAnswerStartedAt();
    Instant getAnswerCompletedAt();
    String getCompletionType();
    Boolean getVideoUploaded();
    String getAnalysisStatus();
}
