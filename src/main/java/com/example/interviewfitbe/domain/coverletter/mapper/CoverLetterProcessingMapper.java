package com.example.interviewfitbe.domain.coverletter.mapper;

import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterProcessingEntity;
import com.example.interviewfitbe.domain.coverletter.enums.ProcessingStatus;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class CoverLetterProcessingMapper {
    public CoverLetterProcessingEntity toEntity(CoverLetterEntity coverLetter) {
        return CoverLetterProcessingEntity.builder()
                .coverLetter(coverLetter)
                .status(ProcessingStatus.PENDING)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
