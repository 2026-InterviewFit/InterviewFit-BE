package com.example.interviewfitbe.domain.coverletter.service;

import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterDto;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.exception.CoverLetterErrorCode;
import com.example.interviewfitbe.domain.coverletter.mapper.CoverLetterMapper;
import com.example.interviewfitbe.domain.coverletter.repository.CoverLetterRepository;
import com.example.interviewfitbe.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CoverLetterService {
    private final CoverLetterRepository coverLetterRepository;
    private final CoverLetterMapper coverLetterMapper;

    public CoverLetterDto getCoverLetter(Long id) {
        return coverLetterMapper.toDto(
                coverLetterRepository.findById(id).orElseThrow(() ->
                        new BaseException(CoverLetterErrorCode.COVER_LETTER_NOT_FOUND))
        );
    }

    public CoverLetterEntity getOrCreate(String content) {
        if (content == null || content.isBlank()) return null;

        return coverLetterRepository.findByContent(content)
                .orElseGet(() -> coverLetterRepository.save(
                        CoverLetterEntity.builder()
//                                .user() // 나중에 유저 추가 필요
                                .content(content)
                                .build()
                ));
    }
}
