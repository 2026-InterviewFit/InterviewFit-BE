package com.example.interviewfitbe.global.exception.code;

import com.example.interviewfitbe.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum FastApiErrorCode implements ErrorCode {
    FRAME_NOT_FOUND(400, "FRAME_NOT_FOUND", "프레임이 존재하지 않습니다."),
    FASTAPI_SERVER_ERROR(500, "FASTAPI_SERVER_ERROR", "분석 서버 오류가 발생했습니다.");

    private final int status;
    private final String code;
    private final String message;

    @Override
    public int getStatus() { return status; }

    @Override
    public String getCode() { return code; }

    @Override
    public String getMessage() { return message; }
}