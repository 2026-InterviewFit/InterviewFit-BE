package com.example.interviewfitbe.domain.coverletter.service.embedding;

import com.example.interviewfitbe.domain.coverletter.dto.embedding.CoverLetterEmbeddingResponse;
import com.example.interviewfitbe.domain.coverletter.entity.*;
import com.example.interviewfitbe.domain.coverletter.repository.CoverLetterQuestionEmbeddingRepository;
import com.example.interviewfitbe.domain.question.entity.QuestionEntity;
import com.example.interviewfitbe.domain.question.enums.QuestionType;
import com.example.interviewfitbe.domain.question.repository.QuestionRepository;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Service
public class CoverLetterQuestionEmbeddingService {
    private final CoverLetterEmbeddingClientService coverLetterEmbeddingClientService;
    private final CoverLetterQuestionEmbeddingRepository coverLetterQuestionEmbeddingRepository;
    private final QuestionRepository questionRepository;

    public void createEmbedding(List<CoverLetterQuestionEntity> coverLetterQuestions) {
        List<String> contents = coverLetterQuestions.stream()
                .map(CoverLetterQuestionEntity::getAnswer)
                .toList();

        CoverLetterEmbeddingResponse response = coverLetterEmbeddingClientService.createCoverLetterEmbedding(contents);

        for (int i = 0; i < coverLetterQuestions.size(); i++) {
            String vector = response.getEmbeddings()
                    .get(i)
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.joining(",", "[", "]"));

            coverLetterQuestionEmbeddingRepository.saveEmbedding(coverLetterQuestions.get(i).getId(), vector);
        }
    }

    public List<QuestionEntity> findPreviousQuestions(UserEntity user, List<CoverLetterQuestionEntity> coverLetterQuestions, List<QuestionEntity> reusableQuestions) {
        if (coverLetterQuestions == null) {
            return List.of();
        }

        List<CoverLetterQuestionEmbeddingEntity> currentEmbeddings = coverLetterQuestionEmbeddingRepository.findAllByCoverLetterQuestionIn(coverLetterQuestions);

        Set<CoverLetterQuestionEmbeddingEntity> similarEmbeddings = new HashSet<>();

        for (CoverLetterQuestionEmbeddingEntity embedding : currentEmbeddings) {
            List<CoverLetterQuestionEmbeddingEntity> similar = coverLetterQuestionEmbeddingRepository.findSimilarByUser(user.getId(), embedding.getEmbedding(), 5);
            similarEmbeddings.addAll(similar);
        }

        List<CoverLetterQuestionEntity> similarCoverLetterQuestions = similarEmbeddings.stream()
                .map(CoverLetterQuestionEmbeddingEntity::getCoverLetterQuestion)
                .distinct()
                .toList();

        if (reusableQuestions.isEmpty()) {
            return questionRepository.findTop6ByCoverLetterQuestionInAndTypeOrderByCreatedAtDesc(similarCoverLetterQuestions, QuestionType.AI_GENERATED);
        }

        return questionRepository.findTop6ByCoverLetterQuestionInAndTypeAndIdNotInOrderByCreatedAtDesc(
                similarCoverLetterQuestions,
                QuestionType.AI_GENERATED,
                reusableQuestions.stream()
                        .map(QuestionEntity::getId)
                        .toList()
        );
    }
}
