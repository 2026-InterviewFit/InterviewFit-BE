package com.example.interviewfitbe.domain.speech_coaching.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 클라이언트 → Spring 스피치 분석 요청 DTO.
 *
 * 변경 전:
 * FastAPI가 분석 완료 후 SpeechReportRequest를 Spring에 Push
 *
 * 변경 후:
 * 클라이언트 또는 Spring 내부 async job이 이 DTO로 Spring 분석 흐름을 실행하고,
 * Spring이 FastAPI를 직접 호출한 뒤 결과를 DB에 저장한다.
 *
 * EyeBlinkingAnalyzeRequest와 동일한 역할.
 */
@Getter
@Setter
@NoArgsConstructor
public class SpeechAnalyzeRequest {

    /**
     * 영상 소스 타입.
     *
     * 예:
     * "minio" | "youtube" | "local"
     */
    private String sourceType;

    /**
     * 영상 소스 값.
     *
     * MinIO 객체 키 또는 YouTube URL 또는 로컬 경로.
     *
     * 예:
     * videos/uuid_sample.mp4
     */
    private String sourceValue;

    /**
     * 직무명.
     *
     * Gemini AI 평가에 사용됨.
     *
     * 예:
     * "백엔드 개발자", "프론트엔드 개발자"
     */
    private String jobCategory;

    /**
     * 면접 세션 ID.
     *
     * 분석 결과를 어떤 interview에 연결할지 결정한다.
     */
    private Long interviewId;

    /**
     * 질문별 답변 영상 ID.
     *
     * 새 구조에서는 speech_report를 interview_answer_id에도 연결해야 하므로 필요하다.
     */
    private Long interviewAnswerId;

    /**
     * 질문 ID.
     *
     * speech 분석 후 답변 텍스트 기반 꼬리질문 생성 시 사용한다.
     */
    private Long questionId;

    /**
     * 원본 질문 텍스트.
     *
     * questionId가 없거나 fallback이 필요할 때 꼬리질문 생성에 사용한다.
     */
    private String originalQuestion;
}