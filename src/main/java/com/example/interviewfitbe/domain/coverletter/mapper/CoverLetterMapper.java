package com.example.interviewfitbe.domain.coverletter.mapper;

import com.example.interviewfitbe.domain.coverletter.dto.CoverLetterDto;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

@Component
public class CoverLetterMapper {
    public CoverLetterDto toDto(CoverLetterEntity entity) {
        return CoverLetterDto.builder()
                .id(entity.getId())
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")))
                .build();
    }
}
