package com.example.interviewfitbe.domain.coverletter.async.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@AllArgsConstructor
public class CoverLetterEmbeddingRetryEvent {
    private Long coverLetterId;
}
