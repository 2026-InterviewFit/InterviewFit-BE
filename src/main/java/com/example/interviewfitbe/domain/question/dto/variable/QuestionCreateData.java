package com.example.interviewfitbe.domain.question.dto.variable;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.job.entity.JobRoleEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QuestionCreateData {
    private JobRoleEntity jobRole;
    private CoverLetterEntity coverLetter;
    private List<CoverLetterQuestionAnalysisEntity> coverLetterQuestionAnalyses;
    private List<String> previousQuestions;
}
