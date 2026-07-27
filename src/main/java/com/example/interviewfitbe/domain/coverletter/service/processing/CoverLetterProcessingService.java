package com.example.interviewfitbe.domain.coverletter.service.processing;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterProcessingEntity;
import com.example.interviewfitbe.domain.coverletter.exception.CoverLetterProcessingErrorCode;
import com.example.interviewfitbe.domain.coverletter.repository.CoverLetterProcessingRepository;
import com.example.interviewfitbe.global.exception.BaseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@RequiredArgsConstructor
@Service
public class CoverLetterProcessingService {
    private final CoverLetterProcessingRepository coverLetterProcessingRepository;

    public CoverLetterProcessingEntity getProcessingByCoverLetter(CoverLetterEntity coverLetter) {
        return coverLetterProcessingRepository.findByCoverLetter(coverLetter).orElseThrow(() ->
                new BaseException(CoverLetterProcessingErrorCode.COVER_LETTER_PROCESSING_NOT_FOUND));
    }
}
