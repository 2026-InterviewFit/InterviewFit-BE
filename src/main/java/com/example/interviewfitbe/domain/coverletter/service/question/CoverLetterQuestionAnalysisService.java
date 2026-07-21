package com.example.interviewfitbe.domain.coverletter.service.question;

import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterQuestionAnalysis;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionAnalysisEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.coverletter.mapper.CoverLetterQuestionAnalysisMapper;
import com.example.interviewfitbe.domain.coverletter.repository.CoverLetterQuestionAnalysisRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@RequiredArgsConstructor
@Service
public class CoverLetterQuestionAnalysisService {
    private final CoverLetterQuestionAnalysisRepository coverLetterQuestionAnalysisRepository;
    private final CoverLetterQuestionAnalysisMapper coverLetterQuestionAnalysisMapper;

    public List<CoverLetterQuestionAnalysisEntity> getCoverLetterQuestionAnalysis(List<CoverLetterQuestionEntity> coverLetterQuestions) {
        return coverLetterQuestionAnalysisRepository.findAllByCoverLetterQuestionIn(coverLetterQuestions);
    }

    public void saveQuestionAnalysis(CoverLetterQuestionAnalysis questionsAnalysis, CoverLetterQuestionEntity coverLetterQuestion) {
        coverLetterQuestionAnalysisRepository.save(
                coverLetterQuestionAnalysisMapper.toEntity(questionsAnalysis, coverLetterQuestion)
        );
    }
}
