package com.example.interviewfitbe.domain.coverletter.exception;

import com.example.interviewfitbe.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CoverLetterProcessingErrorCode  implements ErrorCode {
    COVER_LETTER_PROCESSING_NOT_FOUND(404, "COVER_LETTER_PROCESSING_NOT_FOUND", "자기소개서 저장 로그가 존재하지 않습니다."),
    COVER_LETTER_PROCESSING_NOT_COMPLETED(409, "COVER_LETTER_PROCESSING_NOT_COMPLETED", "자기소개서 처리가 아직 완료되지 않았습니다."),
    COVER_LETTER_PROCESSING_FAILED(422, "COVER_LETTER_PROCESSING_FAILED", "자기소개서 저장 및 분석에 실패했습니다.");

    private final int status;
    private final String code;
    private final String message;

    @Override
    public int getStatus() {
        return status;
    }

    @Override
    public String getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
