package com.example.interviewfitbe.domain.coverletter.service.question;

import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterQuestionAnalysisItem;
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

    public List<CoverLetterQuestionAnalysisEntity> getCoverLetterQuestionAnalyses(List<CoverLetterQuestionEntity> coverLetterQuestions) {
        return coverLetterQuestionAnalysisRepository.findAllByCoverLetterQuestionIn(coverLetterQuestions);
    }

    public void saveQuestionAnalysis(CoverLetterQuestionAnalysisItem questionsAnalysis, CoverLetterQuestionEntity coverLetterQuestion) {
        coverLetterQuestionAnalysisRepository.save(
                coverLetterQuestionAnalysisMapper.toEntity(questionsAnalysis, coverLetterQuestion)
        );
    }
}
