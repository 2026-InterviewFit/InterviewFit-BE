package com.example.interviewfitbe.domain.coverletter.service;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.repository.CoverLetterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CoverLetterService {
    private final CoverLetterRepository coverLetterRepository;

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
