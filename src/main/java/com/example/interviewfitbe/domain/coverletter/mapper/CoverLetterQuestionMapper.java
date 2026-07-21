package com.example.interviewfitbe.domain.coverletter.mapper;

import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterQuestionAnalysis;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import org.springframework.stereotype.Component;

@Component
public class CoverLetterQuestionMapper {
    public CoverLetterQuestionEntity toEntity(CoverLetterQuestionAnalysis questionsAnalysis, CoverLetterEntity coverLetter) {
        return CoverLetterQuestionEntity.builder()
                .coverLetter(coverLetter)
                .answer(questionsAnalysis.getAnswer())
                .question(questionsAnalysis.getQuestion())
                .questionOrder(questionsAnalysis.getQuestionNumber())
                .build();
    }
}
