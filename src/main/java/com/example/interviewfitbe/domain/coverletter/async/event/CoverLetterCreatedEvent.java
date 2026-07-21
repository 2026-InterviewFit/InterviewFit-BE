package com.example.interviewfitbe.domain.coverletter.async.event;

import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterQuestionAnalysis;
import com.example.interviewfitbe.domain.coverletter.entity.CoverLetterEntity;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CoverLetterCreatedEvent {
    private CoverLetterEntity coverLetter;
    private List<CoverLetterQuestionAnalysis> analyses;
}
