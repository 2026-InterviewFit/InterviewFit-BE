package com.example.interviewfitbe.domain.question.exception;

import com.example.interviewfitbe.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum JsonQuestionErrorCode implements ErrorCode {
    QUESTION_DATA_LOAD_FAILED(500, "QUESTION_DATA_LOAD_FAILED", "questions.json을 읽을 수 없습니다.");

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
