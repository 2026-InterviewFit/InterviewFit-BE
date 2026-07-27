package com.example.interviewfitbe.domain.coverletter.async.event;

import com.example.interviewfitbe.domain.coverletter.dto.client.CoverLetterQuestionAnalysisItem;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
public class CoverLetterCreatedEvent {
    private Long coverLetterId;
    private List<CoverLetterQuestionAnalysisItem> analyses;
}
