package com.example.interviewfitbe.domain.question.dto.variable;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CoverLetterQuestionData {
    @Builder.Default
    private List<QuestionEntity> reusableQuestions = List.of();

    @Builder.Default
    private List<CoverLetterQuestionAnalysisEntity> coverLetterQuestionAnalyses = List.of();

    @Builder.Default
    private List<CoverLetterQuestionEntity> coverLetterQuestions = List.of();
}
