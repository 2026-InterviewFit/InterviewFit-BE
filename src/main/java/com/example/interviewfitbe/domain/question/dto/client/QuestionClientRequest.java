package com.example.interviewfitbe.domain.question.dto.client;

import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterQuestionAnalysisResponseItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionClientRequest {
    @JsonProperty("count")
    private Integer count;

    @JsonProperty("job")
    private String jobName;

    @JsonProperty("analyses")
    private List<CoverLetterQuestionAnalysisResponseItem> analyses;

    @JsonProperty("previous_questions")
    private List<String> previousQuestions;
}
