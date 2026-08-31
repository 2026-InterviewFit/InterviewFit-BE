package com.example.interviewfitbe.domain.pt_interview.enums;

public enum PtInterviewStatus {

    CREATED,        // PT 면접 생성
    DOCUMENT_READY, // 발표자료 업로드 및 분석 완료
    READY,          // 면접 시작 가능
    IN_PROGRESS,    // 발표/면접 진행 중
    COMPLETED,      // 면접 완료
    FAILED          // 처리 실패
}