package com.example.interviewfitbe.domain.pose.exception;

import com.example.interviewfitbe.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PoseErrorCode implements ErrorCode {
    POSE_ANALYSIS_RESULT_NOT_FOUND(404, "POSE_ANALYSIS_RESULT_NOT_FOUND", "자세 분석 결과가 존재하지 않습니다.");

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
