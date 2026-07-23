package com.example.interviewfitbe.domain.pt_interview.service;

import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionGenerateRequest;
import com.example.interviewfitbe.domain.pt_interview.dto.PtQuestionGenerateResponse;
import com.example.interviewfitbe.global.exception.BaseException;
import com.example.interviewfitbe.global.exception.code.FastApiErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@RequiredArgsConstructor
@Service
public class PtQuestionAnalysisService {

    private final WebClient fastApiWebClient;

    /*
     * FastAPI PT 예상 질문 생성 호출
     *
     * POST /apis/pt-question/generate
     */
    public PtQuestionGenerateResponse generateQuestions(
            String documentSummary,
            String slidesText,
            Integer questionCount
    ) {

        PtQuestionGenerateRequest request =
                new PtQuestionGenerateRequest(
                        documentSummary,
                        slidesText,
                        questionCount
                );

        try {

            return fastApiWebClient
                    .post()
                    .uri(
                            "/apis/pt-question/generate"
                    )
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(
                            PtQuestionGenerateResponse.class
                    )
                    .toFuture()
                    .join();

        } catch (
                WebClientResponseException e
        ) {

            throw new BaseException(
                    FastApiErrorCode
                            .FASTAPI_SERVER_ERROR
            );
        }
    }
}