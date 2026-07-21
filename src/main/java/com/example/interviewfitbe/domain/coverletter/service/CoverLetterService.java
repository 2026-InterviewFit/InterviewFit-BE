package com.example.interviewfitbe.domain.coverletter.service;

import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterCheckResult;
import com.example.interviewfitbe.domain.coverletter.dto.analysis.CoverLetterAnalysisRequest;
import com.example.interviewfitbe.domain.coverletter.dto.analysis.CoverLetterAnalysisResponse;
import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterClientResponse;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterQuestionEntity;
import com.example.interviewfitbe.domain.coverletter.exception.CoverLetterEmbeddingErrorCode;
import com.example.interviewfitbe.domain.coverletter.exception.CoverLetterErrorCode;
import com.example.interviewfitbe.domain.coverletter.mapper.CoverLetterMapper;
import com.example.interviewfitbe.domain.coverletter.repository.CoverLetterRepository;
import com.example.interviewfitbe.domain.coverletter.service.client.CoverLetterClientService;
import com.example.interviewfitbe.domain.coverletter.service.embedding.CoverLetterQuestionEmbeddingService;
import com.example.interviewfitbe.domain.coverletter.service.question.CoverLetterQuestionService;
import com.example.interviewfitbe.domain.job.entity.RecommendedJobEntity;
import com.example.interviewfitbe.domain.job.service.RecommendJobService;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.domain.user.service.UserService;
import com.example.interviewfitbe.global.exception.BaseException;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
@RequiredArgsConstructor
@Service
public class CoverLetterService {
    private final CoverLetterQuestionService coverLetterQuestionService;
    private final CoverLetterClientService coverLetterClientService;
    private final CoverLetterQuestionEmbeddingService coverLetterQuestionEmbeddingService;
    private final RecommendJobService recommendJobService;
    private final UserService userService;
    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterMapper coverLetterMapper;

    public CoverLetterEntity getCoverLetter(Long id) {
        return coverLetterRepository.findById(id).orElseThrow(() ->
                new BaseException(CoverLetterErrorCode.COVER_LETTER_NOT_FOUND));
    }

    @Transactional
    public CoverLetterAnalysisResponse analyzeCoverLetter(CoverLetterAnalysisRequest request) {
        long start = System.currentTimeMillis();

        UserEntity user = userService.getUserById(request.getUserId());
        log.info("getUser: {}ms", System.currentTimeMillis() - start);

        CoverLetterCheckResult coverLetterCheckResult = getOrCreateCoverLetter(user, request.getCoverLetter());
        log.info("coverLetter: {}ms", System.currentTimeMillis() - start);

        CoverLetterEntity coverLetter = coverLetterCheckResult.getCoverLetter();

        CoverLetterClientResponse analysisResult;
        RecommendedJobEntity recommendedJob;

        if (coverLetterCheckResult.isNew()) {
            analysisResult = coverLetterClientService.analyzeCoverLetter(request.getCoverLetter());
            log.info("gemini: {}ms", System.currentTimeMillis() - start);
            recommendedJob = recommendJobService.saveRecommendedJob(analysisResult, coverLetter);
            log.info("recommendedJob: {}ms", System.currentTimeMillis() - start);

            List<CoverLetterQuestionEntity> coverLetterQuestions = coverLetterQuestionService.saveCoverLetterQuestions(analysisResult.getQuestionsAnalysis(), coverLetter);
            log.info("saveQuestions: {}ms", System.currentTimeMillis() - start);

            coverLetterQuestionEmbeddingService.createEmbedding(coverLetterQuestions);
            log.info("embedding: {}ms", System.currentTimeMillis() - start);
        } else {
            recommendedJob = recommendJobService.getRecommendedJobByCoverLetter(coverLetter);
        }

        return coverLetterMapper.toResponse(coverLetter.getId(), recommendedJob);
    }

    private CoverLetterCheckResult getOrCreateCoverLetter(UserEntity user, String content) {
        if (content == null || content.isBlank()) return null;

        AtomicBoolean isNew = new AtomicBoolean(false);

        CoverLetterEntity entity = coverLetterRepository.findByUserAndContent(user, content)
                .orElseGet(() -> {
                    try {
                        isNew.set(true);
                        return saveCoverLetter(user, content);
                    } catch (SQLException e) {
                        throw new BaseException(CoverLetterEmbeddingErrorCode.COVER_LETTER_EMBEDDING_FAILED);
                    }
                });

        return coverLetterMapper.toCheck(entity, isNew.get());
    }

    private CoverLetterEntity saveCoverLetter(UserEntity user, String content) throws SQLException {
        return coverLetterRepository.save(
                CoverLetterEntity.builder()
                        .user(user)
                        .content(content)
                        .build()
        );
    }
}
