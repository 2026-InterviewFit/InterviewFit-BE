package com.example.interviewfitbe.domain.followup.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class FollowUpTranscriptionResponse {

    /*
     * Whisper STT 결과.
     *
     * FastAPI 응답:
     * {
     *   "text": "사용자의 실제 답변..."
     * }
     */
    private String text;
}