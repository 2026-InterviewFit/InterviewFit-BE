package com.example.interviewfitbe.domain.coverletter.mapper;

import com.example.interviewfitbe.domain.coverletter.dto.analysis.StarDto;
import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterQuestionAnalysis;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.question.dto.client.CoverLetterQuestionAnalysis2;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CoverLetterQuestionAnalysisMapper {
    private final ObjectMapper objectMapper;

    public CoverLetterQuestionAnalysisEntity toEntity(CoverLetterQuestionAnalysis coverLetterQuestionAnalysis, CoverLetterQuestionEntity coverLetterQuestion) {
        return CoverLetterQuestionAnalysisEntity.builder()
                .coverLetterQuestion(coverLetterQuestion)
                .summary(coverLetterQuestionAnalysis.getSummary())
                .starJson(objectMapper.writeValueAsString(coverLetterQuestionAnalysis.getStar()))
                .strengthsJson(objectMapper.writeValueAsString(coverLetterQuestionAnalysis.getStrengths()))
                .improvementsJson(objectMapper.writeValueAsString(coverLetterQuestionAnalysis.getImprovements()))
                .skillsJson(objectMapper.writeValueAsString(coverLetterQuestionAnalysis.getSkills()))
                .keywordsJson(objectMapper.writeValueAsString(coverLetterQuestionAnalysis.getKeywords()))
                .importanceScore(coverLetterQuestionAnalysis.getImportanceScore())
                .build();
    }

    public CoverLetterQuestionAnalysis2 toClient(CoverLetterQuestionAnalysisEntity entity) {
        return CoverLetterQuestionAnalysis2.builder()
                .questionNumber(entity.getCoverLetterQuestion().getQuestionOrder())
                .summary(entity.getSummary())
                .star(objectMapper.readValue(entity.getStarJson(), new TypeReference<StarDto>() {}))
                .strengths(objectMapper.readValue(entity.getStrengthsJson(), new TypeReference<List<String>>() {}))
                .improvements(objectMapper.readValue(entity.getImprovementsJson(), new TypeReference<List<String>>() {}))
                .skills(objectMapper.readValue(entity.getSkillsJson(), new TypeReference<List<String>>() {}))
                .keywords(objectMapper.readValue(entity.getKeywordsJson(), new TypeReference<List<String>>() {}))
                .importanceScore(entity.getImportanceScore())
                .build();
    }
}
