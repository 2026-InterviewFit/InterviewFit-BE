package com.example.interviewfitbe.domain.coverletter.calculator;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CoverLetterQuestionSelector {
    public List<CoverLetterQuestionAnalysisEntity> selectForQuestionGeneration(List<CoverLetterQuestionAnalysisEntity> analyses, List<QuestionEntity> generatedQuestions, int  limit) {
        Map<Long, Long> generatedCountMap =
                generatedQuestions.stream()
                        .filter(question -> question.getCoverLetterQuestion() != null)
                        .collect(Collectors.groupingBy(
                                question -> question.getCoverLetterQuestion().getId(),
                                Collectors.counting()
                        ));

        return analyses.stream()
                .sorted(
                        Comparator.comparingDouble(
                                        (CoverLetterQuestionAnalysisEntity analysis) -> {
                                            long generatedCount = generatedCountMap.getOrDefault(
                                            analysis.getCoverLetterQuestion().getId(),
                                            0L
                                            );

                                            return calculatePriority(
                                                    analysis.getImportanceScore(),
                                                    (int) generatedCount
                                            );
                        }).reversed()
                )
                .limit(limit)
                .toList();
    }

    private double calculatePriority(double importanceScore, int generatedCount) {
        return importanceScore / Math.log(generatedCount + 2);
    }
}
