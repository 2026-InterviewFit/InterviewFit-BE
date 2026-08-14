package com.example.interviewfitbe.domain.coverletter.exception;

import com.example.interviewfitbe.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CoverLetterErrorCode implements ErrorCode {
    COVER_LETTER_FILE_EMPTY(400, "COVER_LETTER_FILE_EMPTY", "자소서 파일이 비어 있습니다."),
    COVER_LETTER_NOT_FOUND(404, "COVER_LETTER_NOT_FOUND", "자기소개서가 존재하지 않습니다.");

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
