package com.example.interviewfitbe.domain.coverletter.service;

import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterDto;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.exception.CoverLetterEmbeddingErrorCode;
import com.example.interviewfitbe.domain.coverletter.exception.CoverLetterErrorCode;
import com.example.interviewfitbe.domain.coverletter.mapper.CoverLetterMapper;
import com.example.interviewfitbe.domain.coverletter.repository.CoverLetterRepository;
import com.example.interviewfitbe.domain.user.entity.UserEntity;
import com.example.interviewfitbe.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.sql.SQLException;

@RequiredArgsConstructor
@Service
public class CoverLetterService {
    private final CoverLetterEmbeddingService coverLetterEmbeddingService;
    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterMapper coverLetterMapper;

    public CoverLetterDto getCoverLetter(Long id) {
        return coverLetterMapper.toDto(
                coverLetterRepository.findById(id).orElseThrow(() ->
                        new BaseException(CoverLetterErrorCode.COVER_LETTER_NOT_FOUND))
        );
    }

    public CoverLetterEntity getOrCreateCoverLetter(UserEntity user, String content) {
        if (content == null || content.isBlank()) return null;

        return coverLetterRepository.findByUserAndContent(user, content)
                .orElseGet(() -> {
                    try {
                        return saveCoverLetter(user, content);
                    } catch (SQLException e) {
                        throw new BaseException(CoverLetterEmbeddingErrorCode.COVER_LETTER_EMBEDDING_FAILED);
                    }
                });
    }

    private CoverLetterEntity saveCoverLetter(UserEntity user, String content) throws SQLException {
        CoverLetterEntity coverLetter = coverLetterRepository.save(
                CoverLetterEntity.builder()
                        .user(user)
                        .content(content)
                        .build()
        );

        coverLetterEmbeddingService.createEmbedding(coverLetter.getId(), content);

        return coverLetter;
    }
}
