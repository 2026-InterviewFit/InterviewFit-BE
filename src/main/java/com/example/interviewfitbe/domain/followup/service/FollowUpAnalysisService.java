package com.example.interviewfitbe.domain.followup.service;

import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.HashMap;
import java.util.Map;

@RequiredArgsConstructor
@Service
public class FollowUpAnalysisService {

    private final WebClient fastApiWebClient;

    /*
     * Spring → FastAPI 꼬리질문 생성 호출
     *
     * Spring 외부 API:
     * POST /api/followup/create
     *
     * FastAPI 내부 API:
     * POST /apis/followup/create
     */
    public String createFollowUpQuestion(
            String job,
            String originalQuestion,
            String answer,
            String interviewStyle
    ) {
        Map<String, Object> request = new HashMap<>();
        request.put("job", job);
        request.put("original_question", originalQuestion);
        request.put("answer", answer);
        request.put("interview_style", interviewStyle);

        try {
            Map<String, Object> response = this.fastApiWebClient.post()
                    .uri("/apis/followup/create")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (response == null) {
                throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
            }

            Object followUpQuestion =
                    response.get("follow_up_question") != null
                            ? response.get("follow_up_question")
                            : response.get("followUpQuestion");

            if (followUpQuestion == null) {
                throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
            }

            return followUpQuestion.toString();

        } catch (WebClientResponseException e) {
            throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
        } catch (Exception e) {
            throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
        }
    }
}