package com.example.interviewfitbe.domain.job.exception;

import com.example.interviewfitbe.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum RecommendedJobErrorCode implements ErrorCode {
    RECOMMENDED_JOB_NOT_FOUND(404, "RECOMMENDED_JOB_NOT_FOUND", "추천 직무를 찾을 수 없습니다.");

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
