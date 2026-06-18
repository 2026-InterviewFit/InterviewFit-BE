package com.example.interviewfitbe.domain.speech_coaching.service;

import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.Map;

/**
 * FastAPI 스피치 분석 호출 전담 서비스.
 * EyeBlinkingAnalysisService와 동일한 패턴으로 FastAPI 호출만 담당한다.
 *
 * 변경 전: FastAPI가 분석 후 Spring에 Push
 * 변경 후: Spring이 FastAPI를 직접 호출하고 결과를 받아옴
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechAnalysisService {

    private final WebClient fastApiWebClient;

    /**
     * FastAPI 스피치 분석 엔드포인트 호출.
     *
     * api.py: api_router = APIRouter(prefix="/apis")
     * api.py: api_router.include_router(speech_router, prefix="/speech")
     * speech_router.py: @router.post("/analyze")
     * 최종 경로: /apis/speech/analyze
     *
     * SpeechAnalyzeRequest가 pydantic 모델이라 FastAPI가 JSON 바디로 받으므로,
     * 쿼리 파라미터 대신 body로 전송해야 한다.
     *
     * @param sourceType  "minio" | "youtube" | "local"
     * @param sourceValue MinIO 객체 키 또는 URL
     * @param jobCategory 직무명 (Gemini 평가에 사용)
     * @param interviewId 면접 세션 ID
     * @return FastAPI 분석 결과 Map
     */
    public Map<String, Object> analyzeSpeech(
            String sourceType,
            String sourceValue,
            String jobCategory,
            Long interviewId
    ) {
        Map<String, Object> requestBody = Map.of(
                "source_type", sourceType,
                "source_value", sourceValue,
                "job_category", jobCategory,
                "interview_id", interviewId
        );

        try {
            Map<String, Object> response = fastApiWebClient.post()
                    .uri("/apis/speech/analyze")
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .timeout(Duration.ofMinutes(5))  // STT + Gemini 처리 시간 고려
                    .block();

            if (response == null) {
                throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
            }

            log.info("FastAPI 스피치 분석 완료 - interviewId: {}", interviewId);
            return response;

        } catch (WebClientResponseException e) {
            log.error("FastAPI 스피치 분석 실패 - status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
        }
    }
}