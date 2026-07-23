package com.example.interviewfitbe.domain.interview.dto;

import com.example.interviewfitbe.domain.interview.enums.AnalysisStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class InterviewAnswerUploadResponse {

    private Long answerId;
    private Long interviewId;
    private Long questionId;
    private Integer answerOrder;

    /**
     * MinIO objectKey.
     * TODO: 배예은의 VideoEntity 리팩토링 이후 videoId로 변경 예정.
     */
    private String videoObjectKey;

    private AnalysisStatus analysisStatus;
}