package com.example.interviewfitbe.domain.coverletter.mapper;

import com.example.interviewfitbe.domain.coverletter.dto.analysis.StarItem;
import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterQuestionAnalysisItem;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterQuestionAnalysisResponseItem;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

@RequiredArgsConstructor
@Component
public class CoverLetterQuestionAnalysisMapper {
    private final ObjectMapper objectMapper;

    public CoverLetterQuestionAnalysisEntity toEntity(CoverLetterQuestionAnalysisItem coverLetterQuestionAnalysisItem, CoverLetterQuestionEntity coverLetterQuestion) {
        return CoverLetterQuestionAnalysisEntity.builder()
                .coverLetterQuestion(coverLetterQuestion)
                .summary(coverLetterQuestionAnalysisItem.getSummary())
                .starJson(objectMapper.writeValueAsString(coverLetterQuestionAnalysisItem.getStar()))
                .strengthsJson(objectMapper.writeValueAsString(coverLetterQuestionAnalysisItem.getStrengths()))
                .improvementsJson(objectMapper.writeValueAsString(coverLetterQuestionAnalysisItem.getImprovements()))
                .skillsJson(objectMapper.writeValueAsString(coverLetterQuestionAnalysisItem.getSkills()))
                .keywordsJson(objectMapper.writeValueAsString(coverLetterQuestionAnalysisItem.getKeywords()))
                .importanceScore(coverLetterQuestionAnalysisItem.getImportanceScore())
                .build();
    }

    public CoverLetterQuestionAnalysisResponseItem toClient(CoverLetterQuestionAnalysisEntity entity) {
        return CoverLetterQuestionAnalysisResponseItem.builder()
                .questionNumber(entity.getCoverLetterQuestion().getQuestionOrder())
                .summary(entity.getSummary())
                .star(objectMapper.readValue(entity.getStarJson(), new TypeReference<StarItem>() {}))
                .strengths(objectMapper.readValue(entity.getStrengthsJson(), new TypeReference<List<String>>() {}))
                .improvements(objectMapper.readValue(entity.getImprovementsJson(), new TypeReference<List<String>>() {}))
                .skills(objectMapper.readValue(entity.getSkillsJson(), new TypeReference<List<String>>() {}))
                .keywords(objectMapper.readValue(entity.getKeywordsJson(), new TypeReference<List<String>>() {}))
                .importanceScore(entity.getImportanceScore())
                .build();
    }
}
