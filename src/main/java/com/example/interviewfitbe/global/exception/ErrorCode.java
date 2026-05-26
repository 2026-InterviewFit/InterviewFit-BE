package com.example.interviewfitbe.global.exception;

public interface ErrorCode {
    int getStatus();
    String getCode();
    String getMessage();
}