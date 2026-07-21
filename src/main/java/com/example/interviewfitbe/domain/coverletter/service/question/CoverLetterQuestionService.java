package com.example.interviewfitbe.domain.coverletter.service.question;

import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterQuestionAnalysis;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.coverletter.mapper.CoverLetterQuestionMapper;
import com.example.interviewfitbe.domain.coverletter.repository.CoverLetterQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@RequiredArgsConstructor
@Service
public class CoverLetterQuestionService {
    private final CoverLetterQuestionAnalysisService coverLetterQuestionAnalysisService;
    private final CoverLetterQuestionRepository coverLetterQuestionRepository;
    private final CoverLetterQuestionMapper coverLetterQuestionMapper;

    public List<CoverLetterQuestionEntity> getCoverLetterQuestions(CoverLetterEntity coverLetter) {
        return coverLetterQuestionRepository.findAllByCoverLetter(coverLetter);
    }

    public List<CoverLetterQuestionEntity> saveCoverLetterQuestions(List<CoverLetterQuestionAnalysis> questionsAnalysis, CoverLetterEntity coverLetter) {
        List<CoverLetterQuestionEntity> savedQuestions = new ArrayList<>();

        for (CoverLetterQuestionAnalysis analysis : questionsAnalysis) {
            CoverLetterQuestionEntity question = coverLetterQuestionRepository.save(
                    coverLetterQuestionMapper.toEntity(analysis, coverLetter)
            );
            coverLetterQuestionAnalysisService.saveQuestionAnalysis(analysis, question);

            savedQuestions.add(question);
        }

        return savedQuestions;
    }
}
