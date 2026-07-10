package com.example.interviewfitbe.domain.question.service;

import com.example.interviewfitbe.domain.question.dto.QuestionClientRequest;
import com.example.interviewfitbe.domain.question.dto.QuestionClientResponse;
import com.example.interviewfitbe.domain.question.exception.QuestionErrorCode;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RequiredArgsConstructor
@Service
public class QuestionClientService {
    private final WebClient fastApiWebClient;

    public QuestionClientResponse createQuestions(Integer count, String jobType, String coverLetter) {
        QuestionClientRequest request = QuestionClientRequest.builder()
                .count(count)
                .jobType(jobType)
                .coverLetter(coverLetter)
                .build();

        try {
            return fastApiWebClient.post()
                    .uri("/apis/question/create")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(QuestionClientResponse.class)
                    .block();
        } catch (WebClientResponseException e) {
            String body = e.getResponseBodyAsString();

            if (body.contains("QUESTION_GENERATION_FAILED")) {
                throw new BaseException(QuestionErrorCode.QUESTION_GENERATION_FAILED);
            }
            if (body.contains("INVALID_QUESTION_RESPONSE")) {
                throw new BaseException(QuestionErrorCode.INVALID_QUESTION_RESPONSE);
            }
            if (body.contains("INVALID_QUESTION_COUNT")) {
                throw new BaseException(QuestionErrorCode.INVALID_QUESTION_COUNT);
            }
            if (body.contains("INVALID_QUESTION_VALIDATION")) {
                throw new BaseException(QuestionErrorCode.INVALID_QUESTION_VALIDATION);
            }
            throw new BaseException(FastApiErrorCode.FASTAPI_SERVER_ERROR);
        }
    }
}
