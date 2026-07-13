package com.example.interviewfitbe.domain.coverletter.service;

import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterEmbeddingResponse;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEmbeddingEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.exception.CoverLetterEmbeddingErrorCode;
import com.example.interviewfitbe.domain.coverletter.repository.CoverLetterEmbeddingRepository;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.enums.QuestionType;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CoverLetterEmbeddingService {
    private final CoverLetterEmbeddingClientService coverLetterEmbeddingClientService;
    private final CoverLetterEmbeddingRepository coverLetterEmbeddingRepository;
    private final QuestionRepository questionRepository;

    public void createEmbedding(Long coverLetterId, String content) {
        CoverLetterEmbeddingResponse response = coverLetterEmbeddingClientService.createCoverLetterEmbedding(content);

        String vectorEmbedding = response.getEmbedding()
                .stream()
                .map(String::valueOf)
                .collect(Collectors.joining(",", "[", "]"));

        coverLetterEmbeddingRepository.saveEmbedding(coverLetterId, vectorEmbedding);

    }

    public List<QuestionEntity> findPreviousQuestions(UserEntity user, CoverLetterEntity coverLetter) {
        if (coverLetter == null) {
            return List.of();
        }

        CoverLetterEmbeddingEntity currentEmbedding = coverLetterEmbeddingRepository.findByCoverLetter(coverLetter)
                        .orElseThrow(() -> new BaseException(
                                CoverLetterEmbeddingErrorCode.COVER_LETTER_EMBEDDING_NOT_FOUND
                        ));

        List<CoverLetterEmbeddingEntity> similarEmbeddings = coverLetterEmbeddingRepository.findSimilarByUser(user.getId(), currentEmbedding.getEmbedding(), 5);

        List<CoverLetterEntity> similarCoverLetters = similarEmbeddings.stream()
                .map(CoverLetterEmbeddingEntity::getCoverLetter)
                .toList();

        return questionRepository.findByCoverLetterInAndType(similarCoverLetters, QuestionType.AI_GENERATED);
    }
}
