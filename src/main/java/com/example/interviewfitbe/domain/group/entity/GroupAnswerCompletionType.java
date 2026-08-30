package com.example.interviewfitbe.domain.group.entity;

public enum GroupAnswerCompletionType {
    COMPLETED,
    TIMED_OUT,
    SKIPPED,
    RECORDING_FAILED;

    public boolean excludesAnalysis() {
        return this == SKIPPED || this == RECORDING_FAILED;
    }
}
