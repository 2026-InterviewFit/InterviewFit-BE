package com.example.interviewfitbe.domain.coverletter.exception;

import com.example.interviewfitbe.global.exception.ErrorCode;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum PdfErrorCode implements ErrorCode {
    PDF_EMPTY(400, "PDF_EMPTY", "업로드된 PDF 파일이 비어 있습니다."),
    PDF_READ_FAILED(500, "PDF_READ_FAILED", "PDF 파일을 읽을 수 없습니다."),
    PDF_IMAGE_EXTRACTION_FAILED(400, "PDF_IMAGE_EXTRACTION_FAILED", "PDF에서 이미지를 추출할 수 없습니다."),
    OCR_FAILED(400, "OCR_FAILED", "PDF에서 텍스트를 추출할 수 없습니다."),
    OCR_SERVER_ERROR(500, "OCR_SERVER_ERROR", "OCR 처리 중 서버 오류가 발생했습니다.");

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
