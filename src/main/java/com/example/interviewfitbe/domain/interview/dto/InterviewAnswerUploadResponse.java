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

    /**
     * 이번 면접에서 사용된 질문 연결 ID.
     * interview_questions.id
     */
    private Long interviewQuestionId;

    /**
     * 재사용 가능한 질문 원본 ID.
     * questions.id
     */
    private Long questionId;

    private Integer answerOrder;

    /**
     * MinIO objectKey.
     * TODO: VideoEntity 리팩토링 이후 videoId로 변경 예정.
     */
    private String videoObjectKey;

    private AnalysisStatus analysisStatus;
}
