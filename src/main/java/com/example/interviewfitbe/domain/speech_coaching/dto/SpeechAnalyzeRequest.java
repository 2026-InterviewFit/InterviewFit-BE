package com.example.interviewfitbe.domain.speech_coaching.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 클라이언트 → Spring 스피치 분석 요청 DTO.
 *
 * 변경 전: FastAPI가 분석 완료 후 SpeechReportRequest를 Spring에 Push
 * 변경 후: 클라이언트가 이 DTO로 Spring을 호출하고,
 *          Spring이 FastAPI를 직접 호출하는 구조
 *
 * EyeBlinkingAnalyzeRequest와 동일한 역할.
 */
@Getter
@Setter
@NoArgsConstructor
public class SpeechAnalyzeRequest {

    /**
     * 영상 소스 타입.
     * "minio" | "youtube" | "local"
     */
    private String sourceType;

    /**
     * 영상 소스 값.
     * MinIO 객체 키 또는 YouTube URL 또는 로컬 경로
     */
    private String sourceValue;

    /**
     * 직무명. Gemini AI 평가에 사용됨.
     * 예: "백엔드 개발자", "프론트엔드 개발자"
     */
    private String jobCategory;

    /**
     * 면접 세션 ID.
     * 분석 결과를 어떤 면접에 연결할지 결정.
     */
    private Long interviewId;
}
