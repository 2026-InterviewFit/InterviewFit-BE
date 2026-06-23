package com.example.interviewfitbe.domain.question.exception;

import com.example.interviewfitbe.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum QuestionErrorCode implements ErrorCode {
    QUESTION_NOT_FOUND(404, "QUESTION_NOT_FOUND", "면접 질문이 존재하지 않습니다."),
    INVALID_QUESTION_RESPONSE(500, "INVALID_GEMINI_RESPONSE", "생성된 면접 질문의 응답 형식이 올바르지 않습니다."),
    QUESTION_GENERATION_FAILED(500, "QUESTION_GENERATION_FAILED", "면접 질문 생성에 실패했습니다.");

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
