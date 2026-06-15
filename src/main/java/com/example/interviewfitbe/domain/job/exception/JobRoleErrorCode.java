package com.example.interviewfitbe.domain.job.exception;

import com.example.interviewfitbe.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum JobRoleErrorCode implements ErrorCode {
    JOB_ROLE_NOT_FOUND(404, "JOB_ROLE_NOT_FOUND", "존재하지 않는 직무입니다.");

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
