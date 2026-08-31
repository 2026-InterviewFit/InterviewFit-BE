package com.example.interviewfitbe.domain.question.dto.variable;

import com.example.interviewfitbe.domain.question.dto.client.QuestionItem;
import com.example.interviewfitbe.domain.question.enums.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionResult {
    private List<QuestionItem> questions;
    private QuestionType type;
}