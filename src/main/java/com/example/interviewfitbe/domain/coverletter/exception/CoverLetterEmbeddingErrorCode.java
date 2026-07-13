package com.example.interviewfitbe.domain.coverletter.exception;

import com.example.interviewfitbe.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum CoverLetterEmbeddingErrorCode implements ErrorCode {
    COVER_LETTER_EMBEDDING_FAILED(500, "COVER_LETTER_EMBEDDING_FAILED", "자기소개서 임베딩 생성에 실패했습니다."),
    COVER_LETTER_EMBEDDING_NOT_FOUND(404, "COVER_LETTER_EMBEDDING_NOT_FOUND", "자기소개서 임베딩 정보를 찾을 수 없습니다.");

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
