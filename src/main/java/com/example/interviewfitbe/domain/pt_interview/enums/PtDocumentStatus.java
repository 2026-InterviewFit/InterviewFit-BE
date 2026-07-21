package com.example.interviewfitbe.domain.pt_interview.enums;

public enum PtDocumentStatus {

    UPLOADED,   // 파일 업로드 완료
    ANALYZING,  // PPT/PDF 분석 중
    COMPLETED,  // 분석 완료
    FAILED      // 분석 실패
}
