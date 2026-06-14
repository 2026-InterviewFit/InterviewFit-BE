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

    /**
     * FastAPI/Gemini 꼬리질문 생성 호출 전담 메서드.
     *
     * followup은 영상이 아니라
     * 원본 질문 + 사용자 답변 텍스트 기반으로 생성된다.
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
                    .uri("/api/followup/create")
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
        }
    }
}