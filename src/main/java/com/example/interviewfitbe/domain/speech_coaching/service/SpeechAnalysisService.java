package com.example.interviewfitbe.domain.speech_coaching.service;

import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

/**
 * FastAPI 스피치 분석 호출 전담 서비스.
 *
 * Spring → FastAPI 요청 담당
 * FastAPI 분석 결과 반환 담당
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SpeechAnalysisService {

    private final WebClient fastApiWebClient;


    /**
     * FastAPI 스피치 분석 호출
     *
     * FastAPI Request:
     *
     * {
     *   source_type,
     *   source_value,
     *   job_category,
     *   interview_id,
     *   interview_answer_id,
     *   question_id,
     *   original_question
     * }
     *
     * @return FastAPI 분석 결과
     */
    public Map<String, Object> analyzeSpeech(
            String sourceType,
            String sourceValue,
            String jobCategory,
            Long interviewId,
            Long interviewAnswerId,
            Long questionId,
            String originalQuestion
    ) {

        Map<String, Object> requestBody = new HashMap<>();

        requestBody.put(
                "source_type",
                sourceType
        );

        requestBody.put(
                "source_value",
                sourceValue
        );

        requestBody.put(
                "job_category",
                jobCategory
        );

        requestBody.put(
                "interview_id",
                interviewId
        );

        requestBody.put(
                "interview_answer_id",
                interviewAnswerId
        );

        requestBody.put(
                "question_id",
                questionId
        );

        requestBody.put(
                "original_question",
                originalQuestion
        );


        try {

            Map<String, Object> response =
                    fastApiWebClient.post()

                            .uri("/apis/speech/analyze")

                            .bodyValue(requestBody)

                            .retrieve()

                            .bodyToMono(
                                    new ParameterizedTypeReference<Map<String, Object>>() {}
                            )

                            .timeout(
                                    Duration.ofMinutes(5)
                            )

                            .block();


            if(response == null){

                throw new BaseException(
                        FastApiErrorCode.FASTAPI_SERVER_ERROR
                );

            }


            log.info(
                    "FastAPI 스피치 분석 완료 - interviewId: {}, answerId: {}",
                    interviewId,
                    interviewAnswerId
            );


            return response;


        } catch (WebClientResponseException e) {


            log.error(
                    "FastAPI 스피치 분석 실패 - status: {}, body: {}",
                    e.getStatusCode(),
                    e.getResponseBodyAsString()
            );


            throw new BaseException(
                    FastApiErrorCode.FASTAPI_SERVER_ERROR
            );
        }
    }
}