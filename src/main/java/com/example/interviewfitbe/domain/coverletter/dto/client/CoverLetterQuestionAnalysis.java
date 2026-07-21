package com.example.interviewfitbe.domain.coverletter.dto.client;

import com.example.interviewfitbe.domain.coverletter.dto.analysis.StarDto;
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
public class CoverLetterQuestionAnalysis {
    @JsonProperty("question_number")
    private Integer questionNumber;

    @JsonProperty("question")
    private String question;

    @JsonProperty("answer")
    private String answer;

    @JsonProperty("summary")
    private String summary;

    @JsonProperty("star")
    private StarDto star;

    @JsonProperty("strengths")
    private List<String> strengths;

    @JsonProperty("improvements")
    private List<String> improvements;

    @JsonProperty("skills")
    private List<String> skills;

    @JsonProperty("keywords")
    private List<String> keywords;

    @JsonProperty("importance_score")
    private Float importanceScore;
}
