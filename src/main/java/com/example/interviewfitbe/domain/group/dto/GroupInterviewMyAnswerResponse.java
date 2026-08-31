package com.example.interviewfitbe.domain.group.dto;

import com.example.interviewfitbe.domain.group.entity.GroupAnalysisStatus;
import com.example.interviewfitbe.domain.group.entity.GroupAnswerCompletionType;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class GroupInterviewMyAnswerResponse {
    private Long answerId;
    private Integer round;
    private Integer questionIndex;
    private Long questionId;
    private String questionText;
    private Instant answerStartedAt;
    private Instant answerCompletedAt;
    private GroupAnswerCompletionType completionType;
    private boolean videoUploaded;
    private GroupAnalysisStatus analysisStatus;
}
