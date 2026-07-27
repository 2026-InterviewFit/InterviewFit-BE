package com.example.interviewfitbe.domain.question.dto.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionItem {
    @JsonProperty("question")
    private String question;

    @JsonProperty("question_number")
    private Integer questionNumber; // 자소서 문항 번호
}
