package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.GroupAnswerCompletionType;

public record GroupAnswerSkipRequest(GroupAnswerCompletionType completionType) {

    public GroupAnswerCompletionType resolvedCompletionType() {
        if (completionType == null) {
            return GroupAnswerCompletionType.SKIPPED;
        }
        if (completionType != GroupAnswerCompletionType.SKIPPED
                && completionType != GroupAnswerCompletionType.RECORDING_FAILED) {
            throw new IllegalArgumentException(
                    "skip completionType은 SKIPPED 또는 RECORDING_FAILED만 사용할 수 있습니다."
            );
        }
        return completionType;
    }
}
